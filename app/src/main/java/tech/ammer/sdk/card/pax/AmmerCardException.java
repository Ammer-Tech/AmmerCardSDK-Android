package tech.ammer.sdk.card.pax;

public class AmmerCardException extends Exception {

    public enum Type {
        COMMUNICATION_ERROR,

        CLA_NOT_SUPPORTED,         //28160
        INSTRUCTION_NOT_SUPPORTED, //27904

        INCORRECT_STATE,           //27013
        INCORRECT_PIN,             //27010
        INCORRECT_DATA,            //27264

        CARD_NOT_SUPPORTED,        //27266
        CARD_ERASED                //-27901
    }

    public AmmerCardException(Type type) {
        super(type.name());
    }

    public AmmerCardException(Type type, Throwable cause) {
        super(type.name(), cause);
    }
}
