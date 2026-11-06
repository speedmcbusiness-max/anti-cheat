# Sentinel – Paper 26.3 anti-cheat
Requires Paper 26.3 + Java 25. Build: `./gradlew build` (needs a Gradle wrapper or system Gradle) -> `build/libs/Sentinel-1.0.0.jar`.

Commands: `/sentinel reload|alerts|vl <player>|reset <player>` (perm `sentinel.admin`). Alerts: `sentinel.alerts`. Exempt: `sentinel.bypass`.

Coverage: Reach, HitBoxes, AutoCrystal, AutoTotem, AutoAnchor, InvMove, Flight/Fly, AutoStunSlam, AntiFallDamage,
Xray, ESP + ItemESP (mitigation), ClickAimAssist, AimAssist, ShieldBreaker, JumpReset, Velocity, NoJumpDelay, NoSlowDown.

Also enable Paper's anti-xray (config/paper-world-defaults.yml -> anticheat.anti-xray, engine-mode 2) - it's the real Xray fix.
Punishments are OFF by default; run in alert-only mode first and tune thresholds in config.yml.
