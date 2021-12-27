package pt.isec.amov.reversi.game.jsonClasses.specialPieces


class RequestExchange {
    var type: String? = null
    var switchToExchange: Int? = null



    constructor(): super() {
        this.type = "REQUEST_EXCHANGE"
    }

    constructor(switchToExchange : Int): super() {
        this.type = "REQUEST_EXCHANGE"
        this.switchToExchange = switchToExchange
    }
}