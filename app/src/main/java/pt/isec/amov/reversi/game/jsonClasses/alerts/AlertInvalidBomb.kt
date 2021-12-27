package pt.isec.amov.reversi.game.jsonClasses.alerts

class AlertInvalidBomb {
    var type: String? = null

    constructor(): super() {
        this.type = "ALERT_INVALID_BOMB"
    }

}