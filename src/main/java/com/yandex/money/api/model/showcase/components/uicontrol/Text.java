package com.yandex.money.api.model.showcase.components.uicontrol;


/**
 * Text field. Specializes {@link TextArea} with optional keyboard layout and pattern.
 *
 * @author Aleksandr Ershov (asershov@yamoney.com)
 */
public class Text extends TextArea {

    /**
     * Constraint represented as regular expression. May be {@code null}.
     * <p/>
     * {@see java.util.regex.Pattern}
     */
    public final String pattern;

    /**
     * Convenient keyboard suggestion. May be {@code null}.
     */
    public final Keyboard keyboard;

    protected Text(Builder builder) {
        super(builder);
        pattern = builder.pattern;
        keyboard = builder.keyboard;
    }

    @Override
    public boolean isValid(String value) {
        return super.isValid(value) && (value == null || value.isEmpty() ||
                (pattern == null || value.matches(pattern)) && !value.contains("\n"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Text text = (Text) o;

        return !(pattern != null ? !pattern.equals(text.pattern) : text.pattern != null) &&
                keyboard == text.keyboard;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (pattern != null ? pattern.hashCode() : 0);
        result = 31 * result + (keyboard != null ? keyboard.hashCode() : 0);
        return result;
    }

    /**
     * Keyboard type. The {@link Keyboard#NUMBER} means that field value can be filled with
     * number keyboard.
     */
    public enum Keyboard {

        NUMBER("number");

        public final String code;

        Keyboard(String code) {
            this.code = code;
        }

        public static Keyboard parse(String code) {
            for (Keyboard keyboard : values()) {
                if (keyboard.code.equals(code)) {
                    return keyboard;
                }
            }
            return null;
        }
    }

    /**
     * {@link Text} builder.
     */
    public static class Builder extends TextArea.Builder {

        private String pattern;
        private Keyboard keyboard;

        @Override
        public Text create() {
            return new Text(this);
        }

        public Builder setPattern(String pattern) {
            this.pattern = pattern;
            return this;
        }

        public Builder setKeyboard(Keyboard keyboard) {
            this.keyboard = keyboard;
            return this;
        }
    }
}
