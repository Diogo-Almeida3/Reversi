package pt.isec.amov.reversi.game.jsonClasses

class OkData {
    var type: String? = null
    var aux : Boolean? =null

    constructor(aux:Boolean): super() {
        this.type = "OK"
        this.aux = aux
    }
}