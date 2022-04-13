package tech.ammer.sdk.card;

public interface CardControllerListener {

    public void onCardAttached();

    public void onAppletSelected();

    public void onAppletNotSelected(String message);

    public void tagDiscoverTimeout();
}
