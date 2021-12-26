package pt.isec.amov.reversi.game.jsonClasses.alerts

class AlertNoValidPlayData {
    var type: String? = null

    constructor(): super() {
        this.type = "ALERT_NO_VALID_PLAYS"
    }
}