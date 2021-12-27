package pt.isec.amov.reversi.game.jsonClasses

class CloseConnection {
    var type: String? = null

    constructor(): super() {
        this.type = "CLOSE_CONNECTION"
    }
}