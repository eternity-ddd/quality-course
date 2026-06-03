package org.eternity.food.base.domain.validation;

/**
 * 검증 결과를 *값으로* 표현. throw 대신 반환을 통해 합성/검사 가능.
 *
 * <ul>
 *   <li>{@link #pass()} — 통과</li>
 *   <li>{@link #fail(String)} — 실패 + 사유</li>
 *   <li>{@link #require()} — 실패 시 ISE throw (terminal)</li>
 * </ul>
 */
public record Check(boolean passed, String reason) {

    public static Check pass() {
        return new Check(true, "");
    }

    public static Check fail(String reason) {
        return new Check(false, reason);
    }

    public void require() {
        if (!passed) {
            throw new IllegalStateException(reason);
        }
    }
}
