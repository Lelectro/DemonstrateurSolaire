package ch.heigvd.huguelet.demonstrateursolaire.data;

import ch.heigvd.huguelet.demonstrateursolaire.utils.TextFormater;

/**
 * TabletCommand
 * - tout est en int.
 */
public class TabletCommand {

    public final static int HORIZONTAL_POSITION_MIN =   100;
    public final static int HORIZONTAL_POSITION_MAX = 15000;

    public final static int VERTICAL_POSITION_MIN =   84;
    public final static int VERTICAL_POSITION_MAX = 3550;

    public static int HORIZONTAL_OFFSET = 0;
    public static int VERTICAL_OFFSET = 0;


    public class TabletCommandInvalidValueException extends Exception {};

    /**
     * 0: mode photo résistance en exploitation,
     * 1: mode GPS / avec valeur (calculée)
     * 3: Mise en position vertical pour des tests,
     * 4: Mise en position plein sud pour des tests,
     * 5: Mode full manuel
     * 6: Roulement
     * 7: Mode photo résistance pour démonstration,
     * 8: Rangement (fin du process)
     * 9: Mode réinitialisation
     */
    public enum Mode {

        LUX(0, "Mode photo-résistance"),
        GPS(1, "Mode GPS"),
        HORIZONTAL_TEST(2, "Test placement horizontal"),
        VERTICAL_TEST(3, "Test placement vertical"),
        CAP_SUD(4, "Test placement direction SUD"),
        MANUAL(5, "Mode manuel"),
        ROULMENT(6, "Test roulement"),
        DEMO_PV(7, "Test photo-résistance"), // TO ADD
        RANGEMENT(8, "Rangement"),
        RESET(9, "Procédure de réinitialisation");

        private final int value;
        private final String name;

        Mode(int value, String name) {
            this.value = value;
            this.name = name;
        }

        public int getValue() {
            return this.value;
        }

        public String getName() {
            return name;
        }


    }

    /*
     * mode (1 position!)
     */
    Mode mode;

    /*
     * Consigne de position horizontale en nbr de pas depuis le zéro fixé à la butée (5 position!)
     * exemple1: 12000
     * exemple2: 00126.
     *
     * Valeur toujours positive.
     */
    int horizontalPosition;

    /*
     * Consigne de position verticale en nbr de pas depuis le zéro fixé à la butée (4 position!)
     * exemple1: 3100
     * exemple2: 00032.
     *
     * Valeur toujours positive.
     */
    int verticalPosition;


    public TabletCommand(Mode mode) {
        this.mode = mode;
        this.horizontalPosition = horizontalPosition;
        this.verticalPosition = verticalPosition;
    }

    public void setHorizontalPosition(int horizontalPosition) throws TabletCommandInvalidValueException {

        horizontalPosition += HORIZONTAL_OFFSET;

        if ( horizontalPosition < HORIZONTAL_POSITION_MIN || HORIZONTAL_POSITION_MAX < horizontalPosition ) {
            throw new TabletCommandInvalidValueException();
        }

        this.horizontalPosition = horizontalPosition;
    }

    public void setVerticalPosition(int verticalPosition) throws TabletCommandInvalidValueException{

        verticalPosition += VERTICAL_OFFSET;

        if ( verticalPosition < VERTICAL_POSITION_MIN || VERTICAL_POSITION_MAX < verticalPosition) {
            throw new TabletCommandInvalidValueException();
        }

        this.verticalPosition = verticalPosition;
    }

    public Mode getMode() {
        return mode;
    }

    public int getHorizontalPosition() {
        return horizontalPosition;
    }

    public int getVerticalPosition() {
        return verticalPosition;
    }

    public String getFullTextCommand () {
        return TextFormater.convertIntoTextWithSpecificSize(1, mode.getValue()) + TextFormater.DEFAULT_DELIM +
                TextFormater.convertIntoTextWithSpecificSize(5, horizontalPosition) + TextFormater.DEFAULT_DELIM +
                TextFormater.convertIntoTextWithSpecificSize(4, verticalPosition) +TextFormater.DEFAULT_DELIM;

    }
}
