package pt.isec.amov.reversi.game.jsonClasses.alerts

class PassPlayData {
    var type: String? = null

    constructor(): super() {
        this.type = "ALERT_PASS"
    }
}