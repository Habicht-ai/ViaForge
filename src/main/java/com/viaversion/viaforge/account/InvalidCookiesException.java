/* Adapted from SkidderClub/Vibe, GPL-3.0. See docs/THIRD_PARTY_NOTICES.md. */
package com.viaversion.viaforge.account;

import java.io.IOException;

/** A definitive cookie/export rejection, distinct from transport or service failures. */
final class InvalidCookiesException extends IOException {
    InvalidCookiesException(String message) { super(message); }
}
