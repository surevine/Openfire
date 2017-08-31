package org.jivesoftware.openfire.user;

import org.jivesoftware.util.StringUtils;

import java.util.Date;
import java.util.StringTokenizer;

public class ResetToken {
    private final String token;
    private final Date expiry;

    public ResetToken(Date expiry, String token) {
        if (expiry == null) {
            this.expiry = new Date();
        } else {
            this.expiry = expiry;
        }
        this.token = token;
    }
    public ResetToken() {
        this.expiry = new Date();
        this.token = StringUtils.randomString(20);
    }
    public ResetToken(String encoded) {
        StringTokenizer toks = new StringTokenizer(encoded, "|");
        long expiry = new Long(toks.nextToken());
        this.expiry = new Date(expiry);
        this.token = toks.nextToken();
    }

    public boolean matches(ResetToken other) {
        if (this.expiry.before(other.expiry) || this.expiry.equals(other.expiry) || other.expiry.equals(new Date(0L))) {
            if (this.token.equals(other.token)) {
                return true;
            }
        }
        return false;
    }

    public String toString() {
        return "" + this.expiry.getTime() + "|" + this.token;
    }
}
