package lilive.jumper

import javax.swing.JRadioButton


class CandidatesOption {
    int type
    String text
    int mnemonic
    JRadioButton radioButton
    String toolTip
    CandidatesOption( int type, String text, int mnemonic, String toolTip ){
        this.type = type
        this.text = text
        this.mnemonic = mnemonic
        this.toolTip = toolTip
    }
}
