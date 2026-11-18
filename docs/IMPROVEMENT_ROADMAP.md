# AliasGame — Technical Audit & Improvement Roadmap

| | |
|---|---|
| Audit date | 2026-11-18 |
| Audited revision | `645dc1d` on `feature/electric-party-ui-polish` (plus an uncommitted whitespace-only change in `GameEngineTest.kt` that belongs to the owner) |
| Scope | All 42 Kotlin files (~4.9k LOC), resources, word assets, Gradle config, GitHub Actions, README, Git history |
| Status | **Roadmap approved by the owner on 2026-11-18 — implementation in progress on `feature/portfolio-quality-improvements`** |

Legend for evidence confidence:

- **Confirmed** — reproduced by running a tool, or follows unambiguously from the code path.
- **High confidence** — follows from code reading plus documented framework behaviour; will be proven by a failing test before fixing.
- **Assumption** — needs verification; marked explicitly.

---

## 1. Baseline verification

| Check | Result | Notes |
|---|---|---|
| `testDebugUnitTest` (local, JDK = Android Studio JBR) | **Passed** — 36 tests, 0 failures | ExampleUnitTest 1, TeamNamesCodecTest 4, GameEngineTest 14, GameViewModelTest 7, SettingsViewModelTest 3, SetupViewModelTest 7. *(Correction: the first audit report said 37 — that number was read from stale XML reports produced before the audit; verified against fresh reports and the source.)* |
| `lintDebug` (local) | **Failed — lint crashed** | `IncompatibleClassChangeError` in `androidx.lifecycle.lint.NonNullableMutableLiveDataDetector` while analysing `GameSessionRepositoryImpl.kt` |
| `assembleDebug` | Not executed separately | `testDebugUnitTest` compiles the main source set successfully |
| `connectedDebugAndroidTest` | Not executed | No emulator in the audit environment |
| GitHub Actions | **Failed on every inspected run** | 9 most recent runs (all branches, incl. `main`) fail at the *Run Lint* step; unit tests, build and instrumented tests are **skipped** in CI |
| Local environment | `JAVA_HOME` not set; `gh` CLI not installed | Gradle was run with `JAVA_HOME` pointing at the Android Studio JBR for the command only |

---

## 2. Professional evaluation (1–10)

| # | Area | Score | Key evidence | What would raise it |
|---|---|---|---|---|
| 1 | Architecture | **6** | Clear `data / domain / presentation` packages, domain interfaces, Hilt wiring, framework-free `GameEngine`. But: engine is a mutable `@Singleton` shared by two ViewModels (`di/AppModule.kt:59-70`); game phase is implicit, spread across 6 booleans in `GameUiState` + `isPaused` in the engine; use-case layer is inconsistent (pass-through `SaveSettingsUseCase`, `GameViewModel` bypasses use cases). | Explicit game-phase model, single owner of game state, consistent (or deliberately removed) use-case layer. |
| 2 | Kotlin code quality | **6** | Idiomatic data/sealed classes, `Result`, `update {}`. Weaknesses: public `var`s in `GameEngine` (`allWords`, `initialTeams`, `settings`), unused `initialTeams`/`repository` params, fully-qualified types inside signatures (`GameViewModel.kt:95-97`), duplicated language mapping (`WordsRepository.kt:28`, `SettingsViewModel.kt:57`), magic defaults (`"EN"`, `"Easy"`) in 5 places. | Encapsulate engine state, centralise constants, remove dead code. |
| 3 | Android practices | **5** | Blocking asset I/O on the main dispatcher (`WordsRepository.kt:37-50`); app locale not persisted on API 24–32 (manifest lacks `AppLocalesMetadataHolderService`); `targetSdk = 34`; `MediaPlayer` created synchronously during composition. Good: `collectAsStateWithLifecycle`, DataStore, splash screen API. | Fix I/O dispatching, locale persistence, target SDK. |
| 4 | Jetpack Compose | **6** | Good: state hoisting into ViewModels, draw-phase reads in `ConfettiEffect`, `graphicsLayer` for entrance animations, content descriptions. Problems: full-screen overlays (`PauseOverlay`, `ReadyOverlay`) do not consume touches → controls underneath stay clickable; `WordCard` reads `offsetY.value` in composition (recomposes every drag frame); 1,143-line `GameScreen.kt`. | Block touches in overlays, split `GameScreen.kt`, defer animated reads. |
| 5 | Business-logic reliability | **4** | Engine scoring/rotation/fair-play/draw logic is correct and tested. However three reachable paths lose a team's turn or change the score while paused (see P0-2..P0-4). | Fix P0 group and cover each phase transition with tests. |
| 6 | Error handling | **5** | Use cases propagate `Result` and rethrow `CancellationException` (good). But: session decode failures silently become `null` (`GameSessionRepositoryImpl.kt:108-113`); restore `catch (e: Exception)` also swallows `CancellationException` (`GameViewModel.kt:74`); raw `exception.message` shown to users (`SetupViewModel.kt:173,212`); `TeamNamesCodec.decode` can throw outside the `catch` (`SettingsRepositoryImpl.kt:69-71`). | Typed errors, logging, localized messages. |
| 7 | Testing | **5** | 36 meaningful unit tests with fakes and `StandardTestDispatcher`; one Compose smoke test. Gaps: no tests for the round/final-word/victory flow in `GameViewModel`, no tests for restore in non-running phases, data layer (JSON parsing, session codec) untested, word assets unvalidated. And CI never actually runs the tests. | Phase-transition VM tests, data-layer tests, asset validation test, green CI. |
| 8 | Performance | **6** | Fine for the app's size. Inefficiencies: session written to DataStore every second with two extra settings reads per write (`GameViewModel.kt:127-147`); word file parsed twice and never cached; `O(n·m)` `usedWords.contains` filter per word (`GameEngine.kt:37`). None is user-visible today. | Persist on meaningful events, cache parsed packs, use a `Set`. |
| 9 | Maintainability | **6** | Small, readable codebase; design tokens exist. Hurting it: `GameScreen.kt` size, magic strings, two divergent Ukrainian word files (`words_ua.json` is dead), implicit state machine. | See P1/P2 tasks. |
| 10 | Documentation | **5** | README is well structured with screenshots, but its *Testing* and *CI* sections are factually outdated (claims 6 engine tests, no UI tests, no instrumented tests in CI). No LICENSE, no sound asset attribution. | Sync README with reality; add LICENSE/attributions. |
| 11 | CI/CD | **3** | Workflow exists and is sensible in shape, but has been red on every inspected run because lint crashes; tests are therefore never executed in CI. Duplicate push+PR runs, full emulator job on every `feature/*` push, no report upload. | Fix lint crash, then tune triggers and artifacts. |
| 12 | **Overall portfolio quality** | **5.5** | Solid foundation and a pleasant UI, but a reviewer who opens the *Actions* tab sees only red, and a short play session can hit P0-2. | P0 + P1 would plausibly bring it to 7–7.5. |

### Interview perspective

- **Good junior portfolio project?** Yes, conditionally. Layering, Hilt, Flow, DataStore, process-death restore and real tests are above typical junior level. The red CI and gameplay-state bugs must be fixed first — they undercut exactly the "reliability" story the project tells.
- **What would impress a senior:** framework-free `GameEngine` with fair-play/draw rules and tests; `CancellationException` handling in use cases; versioned `TeamNamesCodec` with legacy migration; Turbine/`StandardTestDispatcher` usage; session persistence for process death.
- **What would concern them:** red CI; implicit state machine (booleans) leading to impossible states; mutable singleton engine shared across ViewModels; main-thread I/O; README claims that do not match the repo.
- **Likely interview questions:**
  1. Why is `GameEngine` a `@Singleton`? What happens if two `GameViewModel`s exist (e.g. back-stack restore)?
  2. How would you model the game phases so that "paused on the Ready screen" is unrepresentable?
  3. Which dispatcher does `WordsRepositoryImpl.getWords` run on? How would you test that?
  4. Why does `restoreSession` always restore as paused? Walk me through restoring on the round-results screen.
  5. Why are `SaveSettingsUseCase` methods 1:1 with the repository — what value does that layer add?
  6. Why `Channel` for one-off effects instead of state? What happens if an effect is sent while no collector is active?
  7. How does `AppCompatDelegate.setApplicationLocales` persist the locale on API < 33?
- **Address first:** P0-1 (CI), then the P0 game-phase group (P0-2..P0-4).

---

## 3. Findings & prioritized roadmap

Status values: `TODO`, `IN PROGRESS`, `DONE`, `BLOCKED`, `NEEDS DECISION`, `WON'T DO`.

### P0 — Critical bugs / reliability (mandatory)

#### P0-1 · CI is permanently red: lint crashes — `DONE`
- **Problem (Confirmed):** `lintDebug` crashes with `IncompatibleClassChangeError` in `NonNullableMutableLiveDataDetector`. The catalog declares lifecycle `2.8.6` and navigation `2.8.3`, but `androidx.hilt:hilt-navigation-compose:1.3.0` silently upgrades them to lifecycle **2.9.1** / navigation **2.9.0**. The lint checks shipped in lifecycle 2.9.x are compiled against a newer Lint/Kotlin Analysis API than the one in AGP **8.7.2**.
- **Evidence:** `gradle/libs.versions.toml` (`agp = "8.7.2"`, `hiltNavigationCompose = "1.3.0"`); `./gradlew :app:dependencies` shows `lifecycle-runtime-ktx:2.8.6 -> 2.9.1`; GitHub Actions runs fail at *Run Lint*, later steps skipped.
- **Options:**
  - (A) Upgrade AGP (and Gradle wrapper as required) to a version whose lint supports lifecycle 2.9 lint checks. Most future-proof; larger change; exact minimal AGP version must be verified experimentally.
  - (B) Downgrade `hilt-navigation-compose` to `1.2.0` so lifecycle stays on 2.8.x; requires changing the import in `PackSelectionScreen.kt` (`androidx.hilt.lifecycle.viewmodel.compose` does not exist in 1.2.0). Smallest change; keeps old versions.
  - (C) Disable the `NullSafeMutableLiveData` check (project uses no LiveData). Fastest, but hides a toolchain incompatibility — **not recommended**.
- **Also:** declare the real versions in the catalog so resolved versions are no longer a surprise.
- **Benefit:** CI becomes a real quality gate; unit/instrumented tests start running in CI.
- **Risk:** Medium (build tooling). **Complexity:** S (B) / M (A). **Depends on:** —
- **Verification:** `lintDebug`, `testDebugUnitTest`, `assembleDebug` locally; green GitHub Actions run.

#### P0-2 · Pause button is clickable through the Ready overlay → the team loses its whole turn — `DONE` (logic); overlay touch blocking → P0-5
- **Problem (High confidence):** `ReadyOverlay` is a `Box` with only a `background` — it does not consume pointer events, so the Pause button underneath remains clickable. `pauseGame()` has no guard, sets `isPaused = true` → Ready overlay disappears, Pause overlay appears. `resumeGame()` then sets `isTimerRunning = true` with `timeRemaining == 0` → `startTimer()` immediately sends `PlayTimerEndSound` and the state becomes `isLastWordMode` → the team goes straight to the last-word dialog without playing.
- **Evidence:** `GameScreen.kt:235-240, 298-299, 487-500, 817-843`; `GameViewModel.kt:155-166, 209-222`; `GameEngine.kt:151`.
- **Solution:** (1) ViewModel guards: `pauseGame` only while the timer is running; `resumeGame` only resumes a timer that has time left. (2) Overlays consume touches (e.g. a shared `Modifier` that intercepts pointer input) so that hidden controls are not interactive.
- **Risk:** Low. **Complexity:** S. **Depends on:** —
- **Verification:** failing-first `GameViewModelTest` cases (pause on Ready is a no-op; resume with 0s does not enter last-word mode); Compose UI test that the Pause button is not clickable behind the Ready overlay.

#### P0-3 · Answer buttons / swipe remain active while paused — `DONE` (logic); overlay touch blocking → P0-5
- **Problem (High confidence):** `PauseOverlay` also does not consume touches outside its central card, and `onWordSwiped` only checks `timeRemaining > 0`. Tapping *Skip* / *Got it* (bottom of the screen) while paused changes the score and advances words with the timer stopped.
- **Evidence:** `GameScreen.kt:200-207, 213-215, 680-710`; `GameViewModel.kt:168-177`.
- **Solution:** `onWordSwiped` requires an actively running timer and not paused; overlay touch blocking from P0-2.
- **Risk:** Low. **Complexity:** S. **Depends on:** shares the overlay fix with P0-2.
- **Verification:** VM test "swipe while paused does not change score"; UI test.

#### P0-4 · Session restore outside an active timer loses the turn — `DONE`
- **Problem (High confidence):** `restoreSession` always restores `isPaused = true`. If the process died on the **Ready** screen (`timeRemaining == 0`), the user sees the Pause overlay; *Resume* jumps into last-word mode (same mechanism as P0-2). If it died on **Round results**, the engine's `isPaused` is never cleared by `startNextRound()`, so the next team also lands on the Pause overlay → *Resume* → last-word mode.
- **Evidence:** `GameEngine.kt:212-223`; `GameViewModel.kt:60-73, 195-222`; `GameScreen.kt:213-240`. Existing test `init with saved session restores word and stays paused` only covers mid-timer restore.
- **Solution:** restore as paused only when a turn was actually in progress (`0 < timeRemaining < roundTime` / timer had started); otherwise restore to Ready or Round-results; clear pause state on `startNextRound`.
- **Risk:** Medium (session restore). **Complexity:** S–M. **Depends on:** P0-2 guards.
- **Verification:** VM tests for restore in Ready, Running, Last-word, Round-over phases.

#### P0-5 · Overlays do not consume touches — `NEEDS DECISION`
- **Problem:** `ReadyOverlay` and the dimmed area of `PauseOverlay` let taps reach controls underneath. After P0-2/P0-3 this can no longer corrupt game state (the ViewModel rejects out-of-phase actions), but buttons still give haptic feedback and the exit (X) button stays reachable through the Ready overlay.
- **Open question:** blocking all touches also removes the (accidental) ability to tap X on the Ready screen — system Back still works. Owner to decide whether X should remain reachable.
- **Risk:** Low. **Complexity:** S.

### P1 — Important quality / maintainability

| ID | Title | Problem & evidence | Proposed solution | Risk | Size | Depends | Verification |
|---|---|---|---|---|---|---|---|
| P1-1 | Word assets read on the main thread | `WordsRepositoryImpl.getWords/getPacks` do blocking `assets.open` + JSON parse without switching dispatcher; called from `viewModelScope` (Main). File parsed twice, never cached. `data/repository/WordsRepository.kt:31-52` (Confirmed) | Inject an IO `CoroutineDispatcher`, `withContext(io)`, parse once per language and cache; extract parsing into a pure function | Low | S | — | Unit test for parser + dispatcher injection test |
| P1-2 | App language lost after restart on Android 7–12 | `AppCompatDelegate.setApplicationLocales` needs `AppLocalesMetadataHolderService` with `autoStoreLocales=true` in the manifest on API < 33; it is missing. `AndroidManifest.xml`, `SettingsViewModel.kt:52-61` (High confidence, documented behaviour) | Add the service entry (+ `locales_config.xml` for API 33 per-app language settings); centralise language codes (`UA`→`uk`) in one place | Low | S | — | Manual test on API 30 emulator; unit test for code mapping |
| P1-3 | Double-tap can process the final word twice | `onFinalWordProcessed` and `startNextRound` are not idempotent; a second call rolls the turn again (skipping a team). `GameViewModel.kt:179-207` | Guard by current phase (only in last-word / round-over phase) | Low | S | P0 group | VM tests calling twice |
| P1-4 | Implicit game state machine | Phase is derived from 6 booleans + `timeRemaining` + engine `isPaused`; the P0 bugs are symptoms. `GameUiState.kt`, `GameEngine.kt:127-159`, `GameScreen.kt:298` | Introduce an explicit `GamePhase` sealed type (Ready / Running / Paused / LastWord / RoundOver / Finished) owned by the ViewModel; UI renders by phase. **Architectural decision — will ask before starting** | Medium | M | P0 group | Existing + new VM tests, smoke test |
| P1-5 | Mutable singleton `GameEngine` | Shared mutable state between `SetupViewModel` and `GameViewModel`; public `var`s; `currentWord` duplicated between engine and UI state; unused `initialTeams`. `di/AppModule.kt:59-70`, `GameEngine.kt:13-25` | Make engine fields private; single source of truth for current word; document/limit ownership (e.g. setup passes a `GameConfig`, game owns the engine). **Will propose options before changing** | Medium | M | P1-4 | Engine + VM tests |
| P1-6 | `targetSdk = 34` | Google Play requires recent target API levels for updates (35+ since Aug 2025; current requirement must be verified). Raising to 35 enforces edge-to-edge → UI insets must be handled. `app/build.gradle.kts` | Raise `targetSdk`, handle insets, replace deprecated `window.statusBarColor` | Medium | M | P0-1 | Visual check on API 35 emulator, smoke test |
| P1-7 | Critical-path test gaps | No VM tests for swipe→round→final word→victory; no data-layer tests (word JSON parsing, session snapshot codec); no UI test for overlays | Add focused tests alongside each fix; add JVM test that validates all `assets/words_*.json` | Low | M | — | CI |
| P1-8 | README is factually outdated | README says 6 engine tests, no UI tests, CI does not run instrumented tests — all false. `README.md` *Testing*, *Continuous Integration* | Sync README; add CI badge once green | Low | S | P0-1 | Review |
| P1-9 | Generated / personal files tracked in Git | **DONE** — `.kotlin/errors/errors-1789128623845.log` tracked; `.kotlin/` not ignored; `.idea/deploymentTargetSelector.xml` (per-developer) tracked | Untrack (`git rm --cached`), extend `.gitignore` | Low | S | — | `git status` clean after build |

### P2 — Meaningful engineering improvements

| ID | Title | Problem & evidence | Proposed solution | Size |
|---|---|---|---|---|
| P2-1 | Session saved every second; wrong source of language/pack | `syncState()` → `saveSession()` on every tick; each save reads settings twice; snapshot uses *current settings*, not the language/pack the game was started with. `GameViewModel.kt:94-147, 158-161` | Save on meaningful events (+ periodic timer checkpoint); store language/pack in the game session | M |
| P2-2 | Silent error swallowing | Session decode error → `null` without logging; restore `catch (e: Exception)` swallows `CancellationException`; `TeamNamesCodec.decode` throws outside `catch`. `GameSessionRepositoryImpl.kt:107-115`, `GameViewModel.kt:74`, `SettingsRepositoryImpl.kt:69-71` | Log + clear corrupt session, rethrow cancellation, safe decode with fallback | S |
| P2-3 | Localization defects | Raw `exception.message` and English fallbacks shown to users (`SetupViewModel.kt:173,212`, `SetupScreen.kt:419`); `%1$d points` not a plural (uk/ru need plural forms); last word always labelled "(GOT IT)" even when unclaimed (`GameScreen.kt:786`); first-launch language `EN` ignores system locale; 4 unused strings | Localized error strings, `<plurals>`, correct label, default language from system locale | S–M |
| P2-4 | `MediaPlayer` for short SFX | Created synchronously in composition; `start()` on a playing player does not restart → rapid answers lose sounds. `GameScreen.kt:115-137` | `SoundPool` wrapper owned outside composition | S |
| P2-5 | Team-name input has two sources of truth | DataStore re-emission after debounced save can overwrite text the user typed in between (possible cursor jump). `SetupViewModel.kt:81-85, 131-134` (Assumption — needs reproduction) | Load once, then treat UI state as source of truth | S |
| P2-6 | Word data quality | RU *Insane* has only 10 words (vs 100); duplicates within packs (DE 1, UK 1); `words_ua.json` is dead and diverged from `words_uk.json` | Remove dead file, dedupe, flag RU Insane; asset validation test (P1-7) | S |
| P2-7 | Inconsistent DI / use-case layer | `SaveSettingsUseCase` is pure pass-through; `GameViewModel` uses repositories directly; `SetupViewModel` injects unused `repository`; `@Provides` duplicates `@Inject` constructors; mixed `hiltViewModel` imports | Decide one convention (use `@Binds`, drop pass-through or apply consistently) | S–M |
| P2-8 | Timer accuracy & lifecycle | `delay(1000)` loop drifts; each pause/resume restarts a full second (up to +1 s per pause); timer keeps running when the app is backgrounded (product decision) | Deadline-based timer; ask owner whether to auto-pause on background | S |
| P2-9 | CI efficiency | Push + PR duplicate runs on `feature/*`; emulator job on every push; no `concurrency`; no report artifacts; no wrapper validation | Tune triggers, add concurrency, upload lint/test reports, run emulator on PRs to `main` | S |
| P2-10 | Instrumented test isolation | Smoke test deletes DataStore files while the in-process DataStore singleton may keep cached data; real Hilt modules used | Hilt test module / clear via repository API | M |

### P3 — Optional polish

| ID | Title | Notes |
|---|---|---|
| P3-1 | KSP instead of kapt; remove local-machine workarounds from `gradle.properties` (`kapt.use.worker.api=false`, daemon args) | Faster builds; kapt is in maintenance mode |
| P3-2 | Release build hygiene | `isMinifyEnabled = false`, hard-coded `versionCode = 1` |
| P3-3 | Theme consistency | App is visually dark-only but `LightColorScheme` (white surface, black text) is applied in light mode to M3 defaults such as `AlertDialog` |
| P3-4 | Compose micro-performance | `WordCard` reads `offsetY.value` in composition (recompose per drag frame); text auto-size via repeated layout passes |
| P3-5 | `rememberSaveable` for the exit-confirmation dialog state (lost on rotation) | |
| P3-6 | Split `GameScreen.kt` (1,143 lines) into overlay files | Pure move, no behaviour change |
| P3-7 | Repository presentation | LICENSE, sound asset attribution, screenshot names (`strating-game-screen.jpg`, parentheses), remove template `Example*Test`, clean up `main`/`master` duplication and stale branches |
| P3-8 | `usedWords` as `Set`, avoid immediate repeat after reshuffle | |

### Mandatory vs optional

- **Mandatory:** P0-1 … P0-4, P1-1, P1-2, P1-3, P1-7 (tests accompanying fixes), P1-8, P1-9.
- **Recommended, needs owner decision:** P1-4, P1-5, P1-6, P2-8 (background behaviour).
- **Optional refactoring / polish:** remaining P2, all P3.

---

## 4. Implementation plan & decisions

Planned branch: `feature/portfolio-quality-improvements` (not created yet — awaiting approval).

| Order | Task(s) | Commit (planned) |
|---|---|---|
| 1 | P0-1 | `build(deps): fix lint crash caused by transitive lifecycle 2.9 upgrade` |
| 2 | P0-2, P0-3 (VM guards + tests) | `fix(game): ignore pause and answers outside an active turn` |
| 3 | P0-2, P0-3 (overlay touch blocking + UI test) | `fix(ui): block touches behind game overlays` |
| 4 | P0-4 | `fix(game): restore session into the correct game phase` |
| 5 | P1-9 | `chore(git): stop tracking generated and per-developer files` |
| 6 | P1-1 | `fix(data): load word packs off the main thread and cache them` |
| 7 | P1-2 | `fix(settings): persist app locale on Android 12 and below` |
| 8 | P1-3 | `fix(game): make final-word and next-round actions idempotent` |
| 9 | P1-8 | `docs(readme): synchronize testing and CI documentation` |

### Decision log

| Date | Decision | Status |
|---|---|---|
| 2026-11-18 | P0-1: option A chosen by owner. Experimentally, AGP 8.9.3 (+ Gradle 8.11.1, its minimum) is enough to run lifecycle 2.9 lint checks; no jump to AGP 8.10+ or 9.x needed. Catalog now declares the versions that were already being resolved (lifecycle 2.9.1, navigation 2.9.0) — resolved dependency graph verified identical. | Done |
| 2026-11-18 | P0-2..P0-4: fixed in the ViewModel with phase guards (`isWaitingForTurnStart`, `isCountdownActive`) rather than only in the UI, so game rules hold regardless of which control is tapped. Snapshot gained `isTurnInProgress` (default `false`, so older saved sessions still decode; a legacy snapshot with time left is treated as an interrupted turn). Explicit `GamePhase` model (P1-4) deferred — guards are the smallest reliable fix. | Done |

---

## 5. Completed work

| Task | Commit | Tests | Notes |
|---|---|---|---|
| P0-1 Fix lint crash (AGP 8.7.2 → 8.9.3, Gradle 8.9 → 8.11.1, catalog aligned) | `4fb611c` | `lintDebug` passed (0 errors, 60 warnings), `testDebugUnitTest` passed (36/36), `assembleDebug` passed, `assembleDebugAndroidTest` passed; `connectedDebugAndroidTest` not executed (no emulator) | Lint warnings are pre-existing and tracked: GradleDependency 39, UnusedResources 11, PluralsCandidate 5 (P2-3), AndroidGradlePluginVersion 3, RedundantLabel 1, OldTargetApi 1 (P1-6) |
| P0-2, P0-3, P0-4 Phase guards + correct session restore | `a5a2b41` | 8 new `GameViewModelTest` cases written first and confirmed failing (7 failed, 1 legacy-behaviour guard passed), then all passed; `testDebugUnitTest` passed (44/44) | CI for `4fb611c`: Lint, Unit Tests, Build passed on GitHub for the first time |
| P1-9 Untrack `.kotlin/errors/*.log` and `.idea/deploymentTargetSelector.xml`, ignore `.kotlin/` | recorded in next roadmap update | No code change; `git status` verified (local files kept on disk) | `.idea/markdown.xml` (untracked, owner-local) left untouched |

## 6. Remaining technical debt

All items in section 3 are open.
