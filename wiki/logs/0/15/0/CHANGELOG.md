# 0.15.0

Released: 2026-10-30

A dedicated server's owner can now manage the server's API keys in game with `/agents`, and
nobody below permission level 4 can see or run it. Two of core's largest classes were split so
each file has one job; nothing a player sees changes.

## Upgrading

- **Players: replace the core jar.** The `v0.15.0` release carries
  `MCAgentsCoreForge-0.15.0+26.3.jar`, `MCAgentsCoreFabric-0.15.0+26.3.jar` and
  `MCAgentsCoreNeoforge-0.15.0+26.3.jar`; install the one for your loader. Nothing on disk
  changes.
- **Server owners:** `/agents` is available from the console and to operators at level 4.
  A key typed into chat passes through the client and the server log, so prefer the console
  for `token add`.
- **Mods built on core:** no public type changed.

## Added

- **`/agents` on dedicated servers**, on Fabric, NeoForge and Forge: `/agents` or
  `/agents status`, `/agents reload`, `/agents {platform} token add {token}` and
  `/agents {platform} token remove {handle}`. Brigadier's `requires` hides it below permission
  level 4, and every call still passes `ServerEntrypoint`'s authority check. See
  [`wiki/api/token-commands.md`](../../../../api/token-commands.md#the-agents-command).
- **`platforms/server`**, the module that holds `AgentsCommand`, compiled against vanilla
  Minecraft and bundled into every loader jar. It is not published.
- **`ServerEntrypoint.removableHandles(caller, platform)`**, the masked handles `remove`
  suggests, empty for a caller who may not remove a key.

## Changed

- **`MCAgentsProvider`** no longer holds the credential rotation loop; the package-private
  `TokenRotation` does. Requests, retries and messages are unchanged.
- **`FormPanelView`** no longer holds the drop-down a form field opens; the package-private
  `ChoiceList` does. What is drawn and clicked is unchanged.
