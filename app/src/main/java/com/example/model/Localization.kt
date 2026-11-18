package com.example.model

data class LocalizedLabels(
    val play: String,
    val garage: String,
    val careerMode: String,
    val dailyRewards: String,
    val leaderboard: String,
    val settings: String,
    val playerProfile: String,
    val selectMode: String,
    val selectTrack: String,
    val selectCar: String,
    val startRace: String,
    val retry: String,
    val nextRace: String,
    val home: String,
    val paused: String,
    val resume: String,
    val lap: String,
    val speedUnit: String
)

object Localization {
    fun get(lang: AppLanguage): LocalizedLabels = when (lang) {
        AppLanguage.ENGLISH -> LocalizedLabels(
            play = "PLAY",
            garage = "GARAGE",
            careerMode = "CAREER MODE",
            dailyRewards = "DAILY REWARDS",
            leaderboard = "LEADERBOARD",
            settings = "SETTINGS",
            playerProfile = "PLAYER PROFILE",
            selectMode = "SELECT GAME MODE",
            selectTrack = "SELECT TRACK",
            selectCar = "SELECT CAR",
            startRace = "START RACE",
            retry = "RETRY",
            nextRace = "NEXT RACE",
            home = "HOME",
            paused = "RACE PAUSED",
            resume = "RESUME",
            lap = "LAP",
            speedUnit = "KM/H"
        )
        AppLanguage.SPANISH -> LocalizedLabels(
            play = "JUGAR",
            garage = "GARAJE",
            careerMode = "MODO CARRERA",
            dailyRewards = "RECOMPENSAS",
            leaderboard = "CLASIFICACIÓN",
            settings = "AJUSTES",
            playerProfile = "PERFIL",
            selectMode = "MODO DE JUEGO",
            selectTrack = "ELEGIR PISTA",
            selectCar = "ELEGIR COCHE",
            startRace = "INICIAR CARRERA",
            retry = "REINTENTAR",
            nextRace = "SIGUIENTE",
            home = "INICIO",
            paused = "PAUSA",
            resume = "CONTINUAR",
            lap = "VUELTA",
            speedUnit = "KM/H"
        )
        AppLanguage.FRENCH -> LocalizedLabels(
            play = "JOUER",
            garage = "GARAGE",
            careerMode = "MODE CARRIÈRE",
            dailyRewards = "RÉCOMPENSES",
            leaderboard = "CLASSEMENT",
            settings = "PARAMÈTRES",
            playerProfile = "PROFIL JOUEUR",
            selectMode = "MODE DE JEU",
            selectTrack = "CHOISIR CIRCUIT",
            selectCar = "CHOISIR VOITURE",
            startRace = "DÉMARRER",
            retry = "REJOUER",
            nextRace = "SUIVANT",
            home = "ACCUEIL",
            paused = "PAUSE",
            resume = "REPRENDRE",
            lap = "TOUR",
            speedUnit = "KM/H"
        )
        AppLanguage.JAPANESE -> LocalizedLabels(
            play = "プレイ",
            garage = "ガレージ",
            careerMode = "キャリア",
            dailyRewards = "デイリー報酬",
            leaderboard = "ランキング",
            settings = "設定",
            playerProfile = "プロフィール",
            selectMode = "モード選択",
            selectTrack = "コース選択",
            selectCar = "マシン選択",
            startRace = "レース開始",
            retry = "リトライ",
            nextRace = "次のレース",
            home = "ホーム",
            paused = "一時停止",
            resume = "再開",
            lap = "ラップ",
            speedUnit = "KM/H"
        )
    }
}
