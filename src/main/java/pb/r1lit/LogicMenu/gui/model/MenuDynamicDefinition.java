package pb.r1lit.LogicMenu.gui.model;

public class MenuDynamicDefinition {
    private final String provider;
    private final int startSlot;
    private final int endSlot;
    private final int pageSize;
    private final MenuItemDefinition itemTemplate;

    public MenuDynamicDefinition(String provider, int startSlot, int endSlot, int pageSize, MenuItemDefinition itemTemplate) {
        this.provider = provider;
        this.startSlot = startSlot;
        this.endSlot = endSlot;
        this.pageSize = pageSize;
        this.itemTemplate = itemTemplate;
    }

    public String getProvider() {
        return provider;
    }

    public int getStartSlot() {
        return startSlot;
    }

    public int getEndSlot() {
        return endSlot;
    }

    public int getPageSize() {
        return pageSize;
    }

    public MenuItemDefinition getItemTemplate() {
        return itemTemplate;
    }
}

