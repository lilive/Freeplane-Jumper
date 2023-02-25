package lilive.jumper.data


class SNodes extends ArrayList<SNode> implements Cloneable {
    SNodes(){
        super()
    }
    SNodes( ArrayList<SNode> other ){
        super( other )
    }
    String toString(){
        return "SNodes[size:${size()}]"
    }
    SNodes clone(){
        return new SNodes( this )
    }
}
