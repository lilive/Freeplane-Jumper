package lilive.jumper.utils

import org.freeplane.core.util.LogUtils


class LogUtils {

    // For performances insight
    private static long startTime
    private static long lastLogTime

    /**
     * Call this when program starts to use ellapsed time messages later.
     */
    public static void init(){
        startTime = System.currentTimeMillis()
        lastLogTime = startTime
    }

    /**
     * Print a Jumper message at info level in Freeplane log, with optional
     * ellapsed time information.
     *
     * @param message The message to print. It will be prefixed with "JUMPER:"
     * @param printTime True to display ellapsed time information before
     *                  the message. The rest of the parameters are only
     *                  relevants if this parameter is true.
     * @param timeInSeconds True to display the ellapsed time in seconds,
     *                      false to display it in milliseconds.
     * @param printTotalTime True to display also the time ellapsed from the
     *                       start.
     */
    public static void info(
        String message,
        boolean printTime = false,
        boolean timeInSeconds = false,
        boolean printTotalTime = false,
        boolean totalTimeInSeconds = true
    ){
        message = "JUMPER: " + message
        if( printTime ) message = newStep( message, timeInSeconds, printTotalTime, totalTimeInSeconds )
        org.freeplane.core.util.LogUtils.info( message )
    }

    /*
     * Print a Jumper message at warn level in Freeplane log, with optional
     * ellapsed time information.
     * @see info() for parameters documentation.
     */
    public static void warn(
        String message,
        boolean printTime = false,
        boolean timeInSeconds = false,
        boolean printTotalTime = false,
        boolean totalTimeInSeconds = true
    ){
        message = "JUMPER: " + message
        if( printTime ) message = newStep( message, timeInSeconds, printTotalTime, totalTimeInSeconds )
        org.freeplane.core.util.LogUtils.warn( message )
    }

    /*
     * Print a Jumper message at severe level in Freeplane log, with optional
     * ellapsed time information.
     * @see info() for parameters documentation.
     */
    public static void severe(
        String message,
        boolean printTime = false,
        boolean timeInSeconds = false,
        boolean printTotalTime = false,
        boolean totalTimeInSeconds = true
    ){
        message = "JUMPER: " + message
        if( printTime ) message = newStep( message, timeInSeconds, printTotalTime, totalTimeInSeconds )
        org.freeplane.core.util.LogUtils.severe( message )
    }
    
    /**
     * Print the ellapsed time in Jumper at info level in Freeplane log.
     *
     * @see info() for parameters documentation.
     */
    private static void ellapsed(
        boolean timeInSeconds = false,
        boolean printTotalTime = false,
        boolean totalTimeInSeconds = true
    ){
        info( "", true )
    }
    
    /**
     * Store the current time and create a string that other methods
     * can use for log messages.
     * 
     * @see info() for parameters documentation.
     * @return A string that contains the time ellapsed information to
     *         print.
     */
    private static String newStep(
        String message,
        boolean timeInSeconds = false,
        boolean printTotalTime = false,
        boolean totalTimeInSeconds = true
    ){
        long time = System.currentTimeMillis()
        long stepDuration = time - lastLogTime
        lastLogTime = time
        time -= startTime
        
        message += "  -  " 
        if( timeInSeconds ){
            message += "Time ellapsed: ${ stepDuration / 1000 } s"
        } else {
            message += "Time ellapsed: ${stepDuration} ms"
        }

        if( printTotalTime ){
            message += "  -  " 
            if( totalTimeInSeconds ){
                message += "Total time: ${ time / 1000 } s"
            } else {
                message += "Total time: ${time} ms"
            }
        }

        return message;
    }
}

