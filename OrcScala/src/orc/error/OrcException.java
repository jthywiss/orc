//
// OrcException.java -- Java class OrcException
// Project OrcScala
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.error;

import java.io.PrintWriter;
import java.io.StringWriter;

import orc.compile.parse.OrcSourceRange;

/**
 * Any exception generated by Orc, during compilation, loading, or execution.
 * Though sites written in Java will sometimes produce Java-level exceptions,
 * those exceptions are wrapped in a subclass of OrcException to localize and
 * isolate failures (see TokenException, JavaException).
 *
 * @author dkitchin
 */
public abstract class OrcException extends RuntimeException {
    private static final long serialVersionUID = -194692912947820229L;

    OrcSourceRange position;

    /**
     * Constructs a new OrcException with the specified detail message. The
     * cause is not initialized, and may subsequently be initialized by a call
     * to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for later
     *            retrieval by the {@link #getMessage()} method.
     */
    public OrcException(final String message) {
        super(message);
    }

    /**
     * Constructs a new OrcException with the specified detail message and
     * cause.
     * <p>
     * Note that the detail message associated with <code>cause</code> is
     * <i>not</i> automatically incorporated in this throwable's detail message.
     *
     * @param message the detail message (which is saved for later retrieval by
     *            the {@link #getMessage()} method).
     * @param cause the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method). (A <tt>null</tt> value is
     *            permitted, and indicates that the cause is nonexistent or
     *            unknown.)
     */
    public OrcException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new OrcException with the specified cause and a detail
     * message of <tt>(cause==null ? null : cause.toString())</tt> (which
     * typically contains the class and detail message of <tt>cause</tt>).
     *
     * @param cause the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method). (A <tt>null</tt> value is
     *            permitted, and indicates that the cause is nonexistent or
     *            unknown.)
     */
    public OrcException(final Throwable cause) {
        super(cause);
    }

    /**
     * @return
     *         "position: ClassName: detailMessage (newline) position.lineContentWithCaret [if available]"
     */
    public String getMessageAndPositon() {
        if (position != null) {
            return position.toString() + ": " + getClass().getCanonicalName() + ": " + getLocalizedMessage() + (position.lineContentWithCaret().equals("\n^") ? "" : "\n" + position.lineContentWithCaret());
        } else {
            return getLocalizedMessage();
        }
    }

    /**
     * This returns a string with, as applicable and available, position,
     * exception class name, detailed message, the line of source code with a
     * caret, Orc stack trace, Java stack trace, etc. Various subclasses change
     * the format as appropriate.
     *
     * @return String, ending with newline
     */
    public String getMessageAndDiagnostics() {
        return getMessageAndPositon() + "\n";
    }

    /**
     * @param e Throwable to retrieve stack trace from
     * @return The stack trace, as would be printed, without the leading line
     *         "ClassName: detailMessage"
     */
    public String getJavaStacktraceAsString(final Throwable e) {
        final StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        final StringBuffer traceBuf = sw.getBuffer();
        int ind = traceBuf.indexOf("\n\tat ");
        if (ind < 0)
          ind = 0;
        traceBuf.delete(0, ind);
        return traceBuf.toString();
    }

    public OrcSourceRange getPosition() {
        return position;
    }

    public void resetPosition(final OrcSourceRange newpos) {
        position = newpos;
    }

    public OrcException setPosition(final OrcSourceRange newpos) {
        if (position == null) {
            position = newpos;
        }
        return this;
    }

}
