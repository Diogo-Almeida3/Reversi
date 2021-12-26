package pt.isec.amov.reversi.game.jsonClasses.alerts

import pt.isec.amov.reversi.game.PieceMoves

class AlertEndgameData {
    var type: String? = null

    constructor(): super() {
        this.type = "ALERT_ENDGAME"
    }
}