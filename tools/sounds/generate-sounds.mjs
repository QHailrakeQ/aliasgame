// Generates the game's sound effects as WAV files in app/src/main/res/raw.
//
// The sounds are synthesized from scratch (no samples, no third-party audio), so they carry no
// licensing obligations and can be tweaked by editing the parameters below.
//
// Run from the repository root:  node tools/sounds/generate-sounds.mjs
//
// Format: mono, 44.1 kHz, 16-bit PCM. Every sound starts at its first sample (no leading
// silence), which keeps the countdown ticks in sync with the on-screen timer.

import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const SAMPLE_RATE = 44100;
const OUTPUT_DIR = join(dirname(fileURLToPath(import.meta.url)), '..', '..', 'app', 'src', 'main', 'res', 'raw');

// --- Building blocks -------------------------------------------------------------------------

const buffer = (seconds) => new Float64Array(Math.ceil(seconds * SAMPLE_RATE));

/** Adds a decaying tone made of harmonic partials, starting at [start] seconds. */
function addTone(out, { start = 0, freq, partials, attack = 0.004, decay, duration, vibrato = 0 }) {
    const first = Math.floor(start * SAMPLE_RATE);
    const length = Math.min(Math.ceil(duration * SAMPLE_RATE), out.length - first);
    const phases = partials.map(() => 0);
    for (let i = 0; i < length; i++) {
        const t = i / SAMPLE_RATE;
        const attackGain = Math.min(1, t / attack);
        const pitch = freq * (1 + vibrato * Math.sin(2 * Math.PI * 5.5 * t) * Math.min(1, t / 0.25));
        let sample = 0;
        partials.forEach(([multiple, amplitude, partialDecay], p) => {
            phases[p] += (2 * Math.PI * pitch * multiple) / SAMPLE_RATE;
            sample += amplitude * Math.sin(phases[p]) * Math.exp(-t / (partialDecay ?? decay));
        });
        out[first + i] += sample * attackGain;
    }
}

/** Adds a sine whose pitch glides exponentially from [from] to [to] Hz. */
function addGlide(out, { from, to, duration, decay }) {
    let phase = 0;
    const length = Math.min(Math.ceil(duration * SAMPLE_RATE), out.length);
    for (let i = 0; i < length; i++) {
        const t = i / SAMPLE_RATE;
        const freq = from * Math.pow(to / from, t / duration);
        phase += (2 * Math.PI * freq) / SAMPLE_RATE;
        const gain = Math.min(1, t / 0.004) * Math.exp(-t / decay);
        out[i] += gain * (Math.sin(phase) + 0.25 * Math.sin(2 * phase));
    }
}

/** A short burst of noise for the attack of percussive sounds (deterministic for reproducible output). */
function addClick(out, { duration, amplitude }) {
    let seed = 12345;
    const random = () => ((seed = (seed * 1103515245 + 12345) % 2147483648) / 2147483648) * 2 - 1;
    const length = Math.ceil(duration * SAMPLE_RATE);
    let previous = 0;
    for (let i = 0; i < length; i++) {
        const value = random();
        out[i] += amplitude * (value - previous) * (1 - i / length); // differenced noise: bright click
        previous = value;
    }
}

/** One-pole low-pass filter that softens buzzy timbres. */
function lowPass(samples, cutoff) {
    const alpha = 1 - Math.exp((-2 * Math.PI * cutoff) / SAMPLE_RATE);
    let state = 0;
    return samples.map((x) => (state += alpha * (x - state)));
}

/** Fades the last [seconds] out so the sound never ends with a click. */
function fadeOut(samples, seconds = 0.012) {
    const length = Math.ceil(seconds * SAMPLE_RATE);
    for (let i = 0; i < length; i++) samples[samples.length - 1 - i] *= i / length;
    return samples;
}

/** Scales the sound so its loudest sample reaches [peakDb] dBFS. */
function normalize(samples, peakDb) {
    const peak = samples.reduce((max, x) => Math.max(max, Math.abs(x)), 0);
    const gain = Math.pow(10, peakDb / 20) / peak;
    return samples.map((x) => x * gain);
}

function writeWav(name, samples) {
    const data = Buffer.alloc(samples.length * 2);
    samples.forEach((x, i) => data.writeInt16LE(Math.round(Math.max(-1, Math.min(1, x)) * 32767), i * 2));
    const header = Buffer.alloc(44);
    header.write('RIFF', 0);
    header.writeUInt32LE(36 + data.length, 4);
    header.write('WAVE', 8);
    header.write('fmt ', 12);
    header.writeUInt32LE(16, 16); // fmt chunk size
    header.writeUInt16LE(1, 20); // PCM
    header.writeUInt16LE(1, 22); // mono
    header.writeUInt32LE(SAMPLE_RATE, 24);
    header.writeUInt32LE(SAMPLE_RATE * 2, 28); // byte rate
    header.writeUInt16LE(2, 32); // block align
    header.writeUInt16LE(16, 34); // bits per sample
    header.write('data', 36);
    header.writeUInt32LE(data.length, 40);
    mkdirSync(OUTPUT_DIR, { recursive: true });
    writeFileSync(join(OUTPUT_DIR, `${name}.wav`), Buffer.concat([header, data]));
    console.log(`${name}.wav  ${(samples.length / SAMPLE_RATE).toFixed(2)} s`);
}

// --- Timbres ---------------------------------------------------------------------------------

/** Bell: bright attack whose upper partials die away first. */
const bell = (decay) => [[1, 1, decay], [2, 0.4, decay * 0.5], [3, 0.15, decay * 0.3], [4.2, 0.08, decay * 0.2]];

/** Soft square-like tone (odd harmonics) for the "get ready" arpeggio. */
const reed = [[1, 1], [3, 0.3], [5, 0.12], [7, 0.05]];

/** Brass-like tone for the fanfare: many harmonics, filtered afterwards. */
const brass = Array.from({ length: 8 }, (_, n) => [n + 1, 1 / Math.pow(n + 1, 1.1)]);

const NOTE = { C4: 261.63, G4: 392.0, C5: 523.25, E5: 659.25, G5: 783.99, A5: 880.0, C6: 1046.5, E6: 1318.51 };

// --- Sounds ----------------------------------------------------------------------------------
// Peak levels balance the sounds against each other: frequent sounds (ticks, answers) sit
// lower, rare events (turn end, victory) slightly higher.

function correct() {
    // Two quick rising bell notes: a cheerful "ding-ding".
    const out = buffer(0.42);
    addTone(out, { freq: NOTE.A5, partials: bell(0.11), decay: 0.11, duration: 0.42 });
    addTone(out, { start: 0.075, freq: NOTE.E6, partials: bell(0.14), decay: 0.14, duration: 0.345 });
    return normalize(fadeOut(out), -4);
}

function skip() {
    // A soft falling "bloop": clearly different from "correct" without sounding like a penalty.
    const out = buffer(0.24);
    addGlide(out, { from: 520, to: 250, duration: 0.24, decay: 0.09 });
    return normalize(fadeOut(out), -5);
}

function countdownTick() {
    // Woodblock-like knock; the player raises its pitch for the last three seconds.
    const out = buffer(0.09);
    addClick(out, { duration: 0.003, amplitude: 0.5 });
    addTone(out, { freq: 1250, partials: [[1, 1], [2.7, 0.45]], attack: 0.0008, decay: 0.018, duration: 0.09 });
    return normalize(fadeOut(out, 0.008), -8);
}

function turnStart() {
    // Quick rising arpeggio C–E–G–C: "ready, go!".
    const out = buffer(0.62);
    [NOTE.C5, NOTE.E5, NOTE.G5, NOTE.C6].forEach((freq, i) => {
        const start = i * 0.07;
        const last = i === 3;
        addTone(out, { start, freq, partials: reed, decay: last ? 0.16 : 0.06, duration: 0.62 - start });
    });
    return normalize(fadeOut(lowPass(out, 3500)), -4);
}

function timeUp() {
    // Two falling buzzer tones G4 → C4: unmistakably "time is over", but not harsh.
    const out = buffer(0.85);
    addTone(out, { freq: NOTE.G4, partials: brass, attack: 0.01, decay: 0.5, duration: 0.3 });
    addTone(out, { start: 0.3, freq: NOTE.C4, partials: brass, attack: 0.01, decay: 0.35, duration: 0.55 });
    return normalize(fadeOut(lowPass(out, 1800), 0.06), -3);
}

function victory() {
    // Fanfare: C–E–G pickup, then a held C major chord with a bell sparkle on top.
    const out = buffer(2.2);
    [NOTE.C5, NOTE.E5, NOTE.G5].forEach((freq, i) => {
        addTone(out, { start: i * 0.13, freq, partials: brass, attack: 0.012, decay: 0.12, duration: 0.16 });
    });
    const chordStart = 0.39;
    [NOTE.C5, NOTE.E5, NOTE.G5, NOTE.C6].forEach((freq) => {
        addTone(out, { start: chordStart, freq, partials: brass, attack: 0.02, decay: 0.8, duration: 2.2 - chordStart, vibrato: 0.004 });
    });
    addTone(out, { start: chordStart, freq: NOTE.C6 * 2, partials: bell(0.5), decay: 0.5, duration: 1.2 });
    addTone(out, { start: chordStart + 0.12, freq: NOTE.E6 * 2, partials: bell(0.4), decay: 0.4, duration: 1.0 });
    return normalize(fadeOut(lowPass(out, 4000), 0.25), -2);
}

writeWav('correct_sound', correct());
writeWav('skip_sound', skip());
writeWav('countdown_tick_sound', countdownTick());
writeWav('turn_start_sound', turnStart());
writeWav('time_up_sound', timeUp());
writeWav('victory_sound', victory());
