/*
 * @(#)AbstractApplication.java
 *
 * Copyright (c) 1996-2010 by the original authors of JHotDraw
 * and all its contributors.
 * All rights reserved.
 *
 * The copyright of this software is owned by the authors and  
 * contributors of the JHotDraw project ("the copyright holders").  
 * You may not use, copy or modify this software, except in  
 * accordance with the license agreement you entered into with  
 * the copyright holders. For details see accompanying license terms. 
 */
package org.jhotdraw.app;

import java.awt.Container;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URISyntaxException;
import org.jhotdraw.beans.*;
import org.jhotdraw.gui.Worker;
import org.jhotdraw.util.*;
import java.util.prefs.*;
import javax.swing.*;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.jhotdraw.app.action.file.ClearRecentFilesMenuAction;
import org.jhotdraw.app.action.file.LoadDirectoryAction;
import org.jhotdraw.app.action.file.LoadFileAction;
import org.jhotdraw.app.action.file.LoadRecentFileAction;
import org.jhotdraw.app.action.file.OpenRecentFileAction;
import org.jhotdraw.gui.URIChooser;
import org.jhotdraw.util.prefs.PreferencesUtil;

/**
 * This abstract class can be extended to implement an {@link Application}.
 *
 * @author Werner Randelshofer
 * @version $Id: AbstractApplication.java 647 2010-01-24 22:52:59Z rawcoder $
 */
public abstract class AbstractApplication extends AbstractBean implements Application {

    private LinkedList<View> views = new LinkedList<View>();
    private Collection<View> unmodifiableViews;
    private boolean isEnabled = true;
    protected ResourceBundleUtil labels;
    protected ApplicationModel model;
    private Preferences prefs;
    private View activeView;
    public final static String VIEW_COUNT_PROPERTY = "viewCount";
    private LinkedList<URI> recentFiles = new LinkedList<URI>();
    private final static int maxRecentFilesCount = 10;
    private ActionMap actionMap;
    private URIChooser openChooser;
    private URIChooser saveChooser;
    private URIChooser importChooser;
    private URIChooser exportChooser;

    /** Creates a new instance. */
    public AbstractApplication() {
    }

    @Override
    public void init() {
        prefs = PreferencesUtil.userNodeForPackage((getModel() == null) ? getClass() : getModel().getClass());
        int count = prefs.getInt("recentFileCount", 0);
        for (int i = 0; i < count; i++) {
            String path = prefs.get("recentFile." + i, null);
            if (path != null) {
                try {
                    recentFiles.add(new URI(path));
                } catch (URISyntaxException ex) {
                    // Silently don't add this URI
                }
            }
        }
    }

    @Override
    public void start() {
        final View v = createView();
        add(v);
        v.setEnabled(false);
        show(v);

        // Set the start view immediately active, so that
        // ApplicationOpenFileAction picks it up on Mac OS X.
        setActiveView(v);

        v.execute(new Worker<Object>() {

            @Override
            public Object construct() {
                v.clear();
                return null;
            }

            @Override
            public void finished() {
                v.setEnabled(true);
            }
        });
    }

    @Override
    public final View createView() {
        View v = basicCreateView();
        v.setActionMap(createViewActionMap(v));
        return v;
    }

    @Override
    public void setModel(ApplicationModel newValue) {
        ApplicationModel oldValue = model;
        model = newValue;
        firePropertyChange("model", oldValue, newValue);
    }

    @Override
    public ApplicationModel getModel() {
        return model;
    }

    protected View basicCreateView() {
        return model.createView();
    }

    /**
     * Sets the active view. Calls deactivate on the previously
     * active view, and then calls activate on the given view.
     * 
     * @param newValue Active view, can be null.
     */
    public void setActiveView(View newValue) {
        View oldValue = activeView;
        if (activeView != null) {
            activeView.deactivate();
        }
        activeView = newValue;
        if (activeView != null) {
            activeView.activate();
        }
        firePropertyChange(ACTIVE_VIEW_PROPERTY, oldValue, newValue);
    }

    /**
     * Gets the active view.
     * 
     * @return The active view can be null.
     */
    @Override
    public View getActiveView() {
        return activeView;
    }

    @Override
    public String getName() {
        return model.getName();
    }

    @Override
    public String getVersion() {
        return model.getVersion();
    }

    @Override
    public String getCopyright() {
        return model.getCopyright();
    }

    @Override
    public void stop() {
        for (View p : new LinkedList<View>(views())) {
            dispose(p);
        }
    }

    @Override
    public void destroy() {
        stop();
        model.destroyApplication(this);
        System.exit(0);
    }

    @Override
    public void remove(View v) {
        hide(v);
        if (v == getActiveView()) {
            setActiveView(null);
        }
        int oldCount = views.size();
        views.remove(v);
        v.setApplication(null);
        firePropertyChange(VIEW_COUNT_PROPERTY, oldCount, views.size());
    }

    @Override
    public void add(View v) {
        if (v.getApplication() != this) {
            int oldCount = views.size();
            views.add(v);
            v.setApplication(this);
            v.init();
            model.initView(this, v);
            firePropertyChange(VIEW_COUNT_PROPERTY, oldCount, views.size());
        }
    }

    protected abstract ActionMap createViewActionMap(View p);

    @Override
    public void dispose(View view) {
        remove(view);
        model.destroyView(this, view);
        view.dispose();
    }

    @Override
    public Collection<View> views() {
        if (unmodifiableViews == null) {
            unmodifiableViews = Collections.unmodifiableCollection(views);
        }
        return unmodifiableViews;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public void setEnabled(boolean newValue) {
        boolean oldValue = isEnabled;
        isEnabled = newValue;
        firePropertyChange("enabled", oldValue, newValue);
    }

    public Container createContainer() {
        return new JFrame();
    }

    @Override
    public void launch(String[] args) {
        configure(args);
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                init();
                start();
            }
        });
    }

    protected void initLabels() {
        labels = ResourceBundleUtil.getBundle("org.jhotdraw.app.Labels");
    }

    @Override
    public void configure(String[] args) {
    }

    @Override
    public void removePalette(Window palette) {
    }

    @Override
    public void addPalette(Window palette) {
    }

    @Override
    public void removeWindow(Window window) {
    }

    @Override
    public void addWindow(Window window, View p) {
    }

    protected Action getAction(View view, String actionID) {
        return getActionMap(view).get(actionID);
    }

    /** Adds the specified action as a menu item to the supplied menu. */
    protected void addAction(JMenu m, View view, String actionID) {
        addAction(m, getAction(view, actionID));
    }

    /** Adds the specified action as a menu item to the supplied menu. */
    protected void addAction(JMenu m, Action a) {
        if (a != null) {
            if (m.getClientProperty("needsSeparator") == Boolean.TRUE) {
                m.addSeparator();
                m.putClientProperty("needsSeparator", null);
            }
            JMenuItem mi;
            mi = m.add(a);
            mi.setIcon(null);
            mi.setToolTipText(null);
        }
    }

    /** Adds the specified action as a menu item to the supplied menu. */
    protected void addMenuItem(JMenu m, JMenuItem mi) {
        if (mi != null) {
            if (m.getClientProperty("needsSeparator") == Boolean.TRUE) {
                m.addSeparator();
                m.putClientProperty("needsSeparator", null);
            }
            m.add(mi);
        }
    }

    /** Adds a separator to the supplied menu. The separator will only
    be added, if additional items are added using addAction. */
    protected void maybeAddSeparator(JMenu m) {
        m.putClientProperty("needsSeparator", Boolean.TRUE);
    }

    @Override
    public java.util.List<URI> getRecentURIs() {
        return Collections.unmodifiableList(recentFiles);
    }

    @Override
    public void clearRecentURIs() {
        @SuppressWarnings("unchecked")
        java.util.List<URI> oldValue = (java.util.List<URI>) recentFiles.clone();
        recentFiles.clear();
        prefs.putInt("recentFileCount", recentFiles.size());
        firePropertyChange("recentFiles",
                Collections.unmodifiableList(oldValue),
                Collections.unmodifiableList(recentFiles));
    }

    @Override
    public void addRecentURI(URI uri) {
        @SuppressWarnings("unchecked")
        java.util.List<URI> oldValue = (java.util.List<URI>) recentFiles.clone();
        if (recentFiles.contains(uri)) {
            recentFiles.remove(uri);
        }
        recentFiles.addFirst(uri);
        if (recentFiles.size() > maxRecentFilesCount) {
            recentFiles.removeLast();
        }

        prefs.putInt("recentFileCount", recentFiles.size());
        int i = 0;
        for (URI f : recentFiles) {
            prefs.put("recentFile." + i, f.toString());
            i++;
        }

        firePropertyChange("recentFiles", oldValue, 0);
        firePropertyChange("recentFiles",
                Collections.unmodifiableList(oldValue),
                Collections.unmodifiableList(recentFiles));
    }

    protected JMenu createOpenRecentFileMenu(View view) {
        JMenuItem mi;
        JMenu m;

        m = new JMenu();
        labels.configureMenu(m, //
                (getAction(view, LoadFileAction.ID) != null || //
                getAction(view, LoadDirectoryAction.ID) != null) ?//
                "file.loadRecent" ://
                "file.openRecent"//
                );
        m.setIcon(null);
        m.add(getAction(view, ClearRecentFilesMenuAction.ID));

        OpenRecentMenuHandler handler = new OpenRecentMenuHandler(m, view);
        return m;
    }

    /** Updates the menu items in the "Open Recent" file menu. */
    private class OpenRecentMenuHandler implements PropertyChangeListener, Disposable {

        private JMenu openRecentMenu;
        private LinkedList<Action> openRecentActions = new LinkedList<Action>();
        private View view;

        public OpenRecentMenuHandler(JMenu openRecentMenu, View view) {
            this.openRecentMenu = openRecentMenu;
            this.view = view;
            if (view != null) {
                view.addDisposable(this);
            }
            updateOpenRecentMenu();
            addPropertyChangeListener(this);
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            String name = evt.getPropertyName();
            if (name == "recentFiles") {
                updateOpenRecentMenu();
            }
        }

        /**
         * Updates the "File &gt; Open Recent" menu.
         */
        protected void updateOpenRecentMenu() {
            if (openRecentMenu.getItemCount() > 0) {
                JMenuItem clearRecentFilesItem = (JMenuItem) openRecentMenu.getItem(
                        openRecentMenu.getItemCount() - 1);
                openRecentMenu.remove(openRecentMenu.getItemCount() - 1);

                // Dispose the actions and the menu items that are currently in the menu
                for (Action action : openRecentActions) {
                    if (action instanceof Disposable) {
                        ((Disposable) action).dispose();
                    }
                }
                openRecentActions.clear();
                openRecentMenu.removeAll();

                // Create new actions and add them to the menu
                if (getAction(view, LoadFileAction.ID) != null || //
                        getAction(view, LoadDirectoryAction.ID) != null) {
                    for (URI f : getRecentURIs()) {
                        LoadRecentFileAction action = new LoadRecentFileAction(AbstractApplication.this, view, f);
                        openRecentMenu.add(action);
                        openRecentActions.add(action);
                    }
                } else {
                    for (URI f : getRecentURIs()) {
                        OpenRecentFileAction action = new OpenRecentFileAction(AbstractApplication.this, f);
                        openRecentMenu.add(action);
                        openRecentActions.add(action);
                    }
                }
                if (getRecentURIs().size() > 0) {
                    openRecentMenu.addSeparator();
                }

                // Add a separator and the clear recent files item.
                openRecentMenu.add(clearRecentFilesItem);
            }
        }

        @Override
        public void dispose() {
            removePropertyChangeListener(this);
            // Dispose the actions and the menu items that are currently in the menu
            for (Action action : openRecentActions) {
                if (action instanceof Disposable) {
                    ((Disposable) action).dispose();
                }
            }
            openRecentActions.clear();
        }
    }

    @Override
    public URIChooser getOpenChooser(View v) {
        if (v == null) {
            if (openChooser == null) {
                openChooser = model.createOpenChooser(this, null);
                List<URI> ruris = getRecentURIs();
                if (ruris.size() > 0) {
                    try {
                        openChooser.setSelectedURI(ruris.get(0));
                    } catch (IllegalArgumentException e) {
                        // Ignore illegal values in recent URI list.
                    }
                }
            }
            return openChooser;
        } else {
            URIChooser chooser = (URIChooser) v.getComponent().getClientProperty("openChooser");
            if (chooser == null) {
                chooser = model.createOpenChooser(this, v);
                v.getComponent().putClientProperty("openChooser", chooser);
                List<URI> ruris = getRecentURIs();
                if (ruris.size() > 0) {
                    try {
                        chooser.setSelectedURI(ruris.get(0));
                    } catch (IllegalArgumentException e) {
                        // Ignore illegal values in recent URI list.
                    }
                }
            }
            return chooser;
        }
    }

    @Override
    public URIChooser getSaveChooser(View v) {
        if (v == null) {
            if (saveChooser == null) {
                saveChooser = model.createSaveChooser(this, null);
            }
            return saveChooser;
        } else {
            URIChooser chooser = (URIChooser) v.getComponent().getClientProperty("saveChooser");
            if (chooser == null) {
                chooser = model.createSaveChooser(this, v);
                v.getComponent().putClientProperty("saveChooser", chooser);
                try {
                    chooser.setSelectedURI(v.getURI());
                } catch (IllegalArgumentException e) {
                    // ignore illegal values
                }
            }
            return chooser;
        }
    }

    @Override
    public URIChooser getImportChooser(View v) {
        if (v == null) {
            if (importChooser == null) {
                importChooser = model.createImportChooser(this, null);
            }
            return importChooser;
        } else {
            URIChooser chooser = (URIChooser) v.getComponent().getClientProperty("importChooser");
            if (chooser == null) {
                chooser = model.createImportChooser(this, v);
                v.getComponent().putClientProperty("importChooser", chooser);
            }
            return chooser;
        }
    }

    @Override
    public URIChooser getExportChooser(View v) {
        if (v == null) {
            if (exportChooser == null) {
                exportChooser = model.createExportChooser(this, null);
            }
            return exportChooser;
        } else {
            URIChooser chooser = (URIChooser) v.getComponent().getClientProperty("exportChooser");
            if (chooser == null) {
                chooser = model.createExportChooser(this, v);
                v.getComponent().putClientProperty("exportChooser", chooser);
            }
            return chooser;
        }
    }

    /**
     * Sets the application-wide action map.
     */
    public void setActionMap(ActionMap m) {
        actionMap = m;
    }

    /**
     * Gets the action map.
     */
    @Override
    public ActionMap getActionMap(View v) {
        return (v == null) ? actionMap : v.getActionMap();
    }
}
