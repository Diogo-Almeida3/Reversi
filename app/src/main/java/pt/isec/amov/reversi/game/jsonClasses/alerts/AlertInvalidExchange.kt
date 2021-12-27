package pt.isec.amov.reversi.game.jsonClasses.alerts

class AlertInvalidExchange {

    var type: String? = null
    var error : Int? = null
    constructor(): super() {
        this.type = "ALERT_INVALID_EXCHANGE"
    }

    constructor(error: Int): super() {
        this.type = "ALERT_INVALID_EXCHANGE"
        this.error = error
    }
}