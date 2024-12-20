/*
 * Copyright (C) 2004-2008 Jive Software, 2017-2024 Ignite Realtime Foundation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jivesoftware.openfire.muc;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Exception used for representing that the user is not allowed to perform the requested operation
 * in the MUCRoom. There are many reasons why a not-allowed error could occur such as: a user tries
 * to join a room that has reached its limit of max number of occupants, or attempts to create a
 * room that has been retired. A 405 error code is returned to the user that requested the
 * invalid operation.
 *
 * @author Gaston Dombiak
 */
public class NotAllowedException extends Exception {

    private static final long serialVersionUID = 1L;

    public enum Reason {
        INSUFFICIENT_PERMISSIONS,  // Default reason
        ROOM_RETIRED
    }

    private final Reason reason;

    private Throwable nestedThrowable = null;

    public NotAllowedException() {
        this(Reason.INSUFFICIENT_PERMISSIONS);
    }

    public NotAllowedException(String msg) {
        this(msg, Reason.INSUFFICIENT_PERMISSIONS);
    }

    public NotAllowedException(Throwable nestedThrowable) {
        this(Reason.INSUFFICIENT_PERMISSIONS, nestedThrowable);
    }

    public NotAllowedException(String msg, Throwable nestedThrowable) {
        this(msg, Reason.INSUFFICIENT_PERMISSIONS, nestedThrowable);
    }

    public NotAllowedException(Reason reason) {
        super();
        this.reason = reason;
    }

    public NotAllowedException(String msg, Reason reason) {
        super(msg);
        this.reason = reason;
    }

    public NotAllowedException(Reason reason, Throwable nestedThrowable) {
        this.reason = reason;
        this.nestedThrowable = nestedThrowable;
    }

    public NotAllowedException(String msg, Reason reason, Throwable nestedThrowable) {
        super(msg);
        this.reason = reason;
        this.nestedThrowable = nestedThrowable;
    }

    public Reason getReason() {
        return reason;
    }

    @Override
    public void printStackTrace() {
        super.printStackTrace();
        if (nestedThrowable != null) {
            nestedThrowable.printStackTrace();
        }
    }

    @Override
    public void printStackTrace(PrintStream ps) {
        super.printStackTrace(ps);
        if (nestedThrowable != null) {
            nestedThrowable.printStackTrace(ps);
        }
    }

    @Override
    public void printStackTrace(PrintWriter pw) {
        super.printStackTrace(pw);
        if (nestedThrowable != null) {
            nestedThrowable.printStackTrace(pw);
        }
    }
}
