data
 ┣ local
 │  ┣ LocalUserDataSource.kt
 │  ┣ LudiaryDatabase.kt
 │  ┣ dao
 │  │   ├─ UserDao.kt
 │  │   ├─ GameBaseDao.kt            // NUEVO
 │  │   ├─ UserGameDao.kt            // NUEVO
 │  │   ├─ GameSuggestionDao.kt      // NUEVO
 │  │   └─ PendingOperationDao.kt    // NUEVO
 │  ┣ entity
 │  │   ├─ UserEntity.kt
 │  │   ├─ GameBaseEntity.kt         // NUEVO
 │  │   ├─ UserGameEntity.kt         // NUEVO
 │  │   ├─ GameSuggestionEntity.kt   // NUEVO
 │  │   ├─ PendingOperationEntity.kt // NUEVO
 │  │   └─ LudiaryTypeConverters.kt  // NUEVO (listas, enums…)
 ┣ model
 │  ├─ Session.kt
 │  ├─ User.kt
 │  ├─ UserGame.kt                   // lo adaptamos al modelo que definimos
 │  ├─ GameBase.kt                   // NUEVO
 │  ├─ GameSuggestion.kt             // NUEVO
 │  └─ enums
 │      └─ LudotecaEnums.kt          // NUEVO: GameType, GameCondition, SyncStatus...
 ┗ repository
    ├─ AuthRepository.kt
    ├─ FirebaseAuthRepository.kt
    ├─ FirestoreProfileRepository.kt
    ├─ ProfileRepository.kt
    └─ GamesRepository.kt            // NUEVO: lógicas de Ludoteca (local+Firebase)
