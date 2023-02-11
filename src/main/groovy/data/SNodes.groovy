package lilive.jumper.data


class SNodes extends ArrayList<SNode> {
    SNodes(){
        super()
    }
    SNodes( ArrayList<SNode> other ){
        super( other )
    }
    String toString(){
        return "SNodes[size:${size()}]"
    }
}
