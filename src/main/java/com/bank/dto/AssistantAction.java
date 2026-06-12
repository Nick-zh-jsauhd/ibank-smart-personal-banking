package com.bank.dto;

public class AssistantAction {
    private String label;
    private String href;
    private String style;

    public AssistantAction() {
    }

    public AssistantAction(String label, String href, String style) {
        this.label = label;
        this.href = href;
        this.style = style;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }
}
