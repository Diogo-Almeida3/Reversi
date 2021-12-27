package pt.isec.amov.reversi.game.jsonClasses.profile

class ProfileData {
    var type: String? = null
    var name: String? = null
    var photo: String? = null

    constructor() : super() {}

    constructor(name: String, photo: String) : super() {
        this.type = "PROFILE"
        this.name = name
        this.photo = photo
    }
}