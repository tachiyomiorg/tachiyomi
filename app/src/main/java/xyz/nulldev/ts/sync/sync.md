## Sync Framework

Sync between Tachiyomi and TachiWeb is a relatively simple process!
The exact steps are detailed below.

1. Client uploads it's current library state to the server
2. Client uploads it's library state saved the last time it synced successfully
3. Server compares these two library states for changes
4. Server replays these changes onto it's master library state
5. Server sends it's master library state back to the client
6. Client applies the new master library state and makes a copy of this library state (used in step 2)