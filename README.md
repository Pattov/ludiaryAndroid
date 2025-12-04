data
 ┣ local
 │  ┣ LocalUserDataSource.kt
 │  ┣ LocalUserGamesDataSource.kt
 │  ┣ LudiaryConverters.kt
 │  ┣ LudiaryDatabase.kt
 │  ┣ dao
 │  │   ├─ UserDao.kt
 │  │   ├─ GameBaseDao.kt            
 │  │   ├─ UserGameDao.kt            
 │  │   └─ GameSuggestionDao.kt      
 │  ┣ entity
 │  │   ├─ UserEntity.kt
 │  │   ├─ GameBaseEntity.kt         
 │  │   ├─ UserGameEntity.kt         
 │  │   ├─ GameSuggestionEntity.kt   
 │  │   └─ LudiaryTypeConverters.kt  
 ┣ model
 │  ├─ Session.kt
 │  ├─ User.kt
 │  ├─ UserGame.kt                   
 │  ├─ GameBase.kt                   
 │  ├─ GameSuggestion.kt             
 │  └─ enums
 ┗ repository
    ├─ AuthRepository.kt
    ├─ FirebaseAuthRepository.kt
    ├─ FirestoreProfileRepository.kt
    ├─ FirestoreUserGamesRepository.kt
    ├─ ProfileRepository.kt
    ├─ UserGamesRepositoryImpl.kt
    └─ UserGamesRepository.kt       
