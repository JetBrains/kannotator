These annotations were removed. They were incorrect. There are some informal comments about them.

Documentation directly says that it may be null

    <item name="java.awt.TrayIcon void displayMessage(java.lang.String, java.lang.String, java.awt.TrayIcon.MessageType) 1">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

    <item name="java.awt.image.BandedSampleModel int[] getPixel(int, int, int[], java.awt.image.DataBuffer) 2">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

Directly follows from code!

    <item name="java.awt.image.DirectColorModel java.lang.Object getDataElements(int, java.lang.Object) 1">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

    <item name="java.awt.image.IndexColorModel java.lang.Object getDataElements(int, java.lang.Object) 1">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>


code + docs

    <item name="java.awt.image.MultiPixelPackedSampleModel int[] getPixel(int, int, int[], java.awt.image.DataBuffer) 2">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

follows from code

    <item name="java.beans.Encoder java.beans.PersistenceDelegate getPersistenceDelegate(java.lang.Class&lt;?&gt;)">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

    <item name="java.io.CharArrayWriter java.io.Writer append(java.lang.CharSequence, int, int) 0">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

from code + docs

    <item name="java.io.PrintStream java.io.PrintStream append(java.lang.CharSequence, int, int) 0">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

    <item name="java.io.PrintStream java.lang.Appendable append(java.lang.CharSequence, int, int) 0">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

    <item name="java.io.PrintWriter java.io.PrintWriter append(java.lang.CharSequence, int, int) 0">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

    <item name="java.io.PrintWriter java.io.Writer append(java.lang.CharSequence, int, int) 0">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

    <item name="java.io.PrintWriter java.lang.Appendable append(java.lang.CharSequence, int, int) 0">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

    <item name="java.io.StringWriter java.io.Writer append(java.lang.CharSequence, int, int) 0">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

    <item name="java.io.StringWriter java.lang.Appendable append(java.lang.CharSequence, int, int) 0">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

    <item name="java.io.StringWriter void write(java.lang.String) 0">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

    <item name="java.io.Writer java.lang.Appendable append(java.lang.CharSequence, int, int) 0">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

    <item name="java.lang.AbstractStringBuilder java.lang.Appendable append(java.lang.CharSequence, int, int) 0">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

    <item name="java.lang.Appendable java.lang.Appendable append(java.lang.CharSequence, int, int) 0">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

    <item name="java.lang.Boolean boolean getBoolean(java.lang.String) 0">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

may return null

    <item name="java.lang.ClassLoader java.lang.Package getPackage(java.lang.String)">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

may return null -- see implementation EnsureInitialized

    <item name="java.lang.ClassValue T computeValue(java.lang.Class&lt;?&gt;)">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

scala> java.lang.Integer.getInteger(null, null)
res52: Integer = null

just simple testing

    <item name="java.lang.Integer java.lang.Integer getInteger(java.lang.String) 0">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>
    <item name="java.lang.Integer java.lang.Integer getInteger(java.lang.String, int) 0">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>
    <item name="java.lang.Integer java.lang.Integer getInteger(java.lang.String, java.lang.Integer) 0">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

the same

    <item name="java.lang.Long java.lang.Long getLong(java.lang.String) 0">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>
    <item name="java.lang.Long java.lang.Long getLong(java.lang.String, java.lang.Long) 0">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>
    <item name="java.lang.Long java.lang.Long getLong(java.lang.String, long) 0">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>


    <item name="java.lang.StringBuffer java.lang.AbstractStringBuilder append(java.lang.CharSequence, int, int) 0">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

    <item name="java.lang.StringBuffer java.lang.Appendable append(java.lang.CharSequence, int, int) 0">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

    <item name="java.lang.StringBuilder java.lang.AbstractStringBuilder append(java.lang.CharSequence, int, int) 0">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

    <item name="java.lang.StringBuilder java.lang.Appendable append(java.lang.CharSequence, int, int) 0">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

scala> System.setSecurityManager(null) - OK

    <item name="java.lang.System void setSecurityManager(java.lang.SecurityManager) 0">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

see code

    <item name="java.lang.management.MonitorInfo MonitorInfo(java.lang.String, int, int, java.lang.StackTraceElement) 3">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>


    <item name="java.nio.CharBuffer java.lang.Appendable append(java.lang.CharSequence, int, int) 0">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

    <item name="java.nio.CharBuffer java.nio.CharBuffer append(java.lang.CharSequence, int, int) 0">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

see code

    <item name="java.nio.charset.Charset Charset(java.lang.String, java.lang.String[]) 1">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>


    <item name="java.nio.file.FileSystems java.nio.file.FileSystem newFileSystem(java.net.URI, java.util.Map&lt;java.lang.String,?&gt;, java.lang.ClassLoader) 2">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>


see documentation

    <item name="java.security.KeyStore void setKeyEntry(java.lang.String, java.security.Key, char[], java.security.cert.Certificate[]) 3">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>


see docs

    <item name="java.util.concurrent.ForkJoinWorkerThread void onTermination(java.lang.Throwable) 0">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

see docs

    <item name="java.util.concurrent.locks.ReentrantLock java.lang.Thread getOwner()">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

see docs

    <item name="java.util.concurrent.locks.ReentrantReadWriteLock java.lang.Thread getOwner()">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

see code

    <item name="javax.accessibility.AccessibleHyperlink java.lang.Object getAccessibleActionObject(int)">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

need to read code

    <item name="javax.activation.CommandInfo java.lang.Object getCommandObject(javax.activation.DataHandler, java.lang.ClassLoader)">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

see docs

    <item name="javax.management.remote.rmi.RMIConnection java.lang.Integer[] addNotificationListeners(javax.management.ObjectName[], java.rmi.MarshalledObject[], javax.security.auth.Subject[]) 2">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

see code

    <item name="javax.management.remote.rmi.RMIConnectionImpl java.lang.Integer[] addNotificationListeners(javax.management.ObjectName[], java.rmi.MarshalledObject[], javax.security.auth.Subject[]) 2">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

see docs

    <item name="javax.management.remote.rmi.RMIConnector javax.management.remote.JMXServiceURL getAddress()">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

see docs

    <item name="org.w3c.dom.Node org.w3c.dom.NamedNodeMap getAttributes()">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

see docs

    <item name="javax.swing.JInternalFrame javax.swing.JDesktopPane getDesktopPane()">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

conflict

    <item name="org.w3c.dom.Element org.w3c.dom.NodeList getElementsByTagName(java.lang.String)">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

conflict

    <!-- really may be null, just call it with null -->
    <item name="javax.swing.SwingUtilities java.awt.Component findFocusOwner(java.awt.Component)">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

DefaultElement may return null

    <item name="org.w3c.dom.Element org.w3c.dom.NodeList getElementsByTagNameNS(java.lang.String, java.lang.String)">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>


really may return null if i != 0, conflict

    <item name="javax.swing.JEditorPane.JEditorPaneAccessibleHypertextSupport.HTMLLink java.lang.Object getAccessibleActionObject(int)">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

see docs - may return null

    <item name="javax.management.modelmbean.RequiredModelMBean java.lang.Object getAttribute(java.lang.String)">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

may return null - XPathNamespaceImpl

    <item name="org.w3c.dom.Node org.w3c.dom.Node insertBefore(org.w3c.dom.Node, org.w3c.dom.Node)">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

see LegacyHookGetFields

    <!-- needs to analyze workflow of final fields in GetField impl -->
    <item name="java.io.ObjectInputStream.GetField java.io.ObjectStreamClass getObjectStreamClass()">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

see DOM2DTMdefaultNamespaceDeclarationNode

    <item name="org.w3c.dom.Node org.w3c.dom.NodeList getChildNodes()">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

need to analyze sun/oracle classes

    <item name="java.awt.Image java.lang.Object getProperty(java.lang.String, java.awt.image.ImageObserver)">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>


see XPathNamespaceImpl

    <item name="org.w3c.dom.Node org.w3c.dom.Node removeChild(org.w3c.dom.Node)">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

see docs

    <item name="java.lang.reflect.Proxy java.lang.Class&lt;?&gt; getProxyClass(java.lang.ClassLoader, java.lang.Class&lt;?&gt;...)">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

    <item name="org.w3c.dom.Node org.w3c.dom.Node cloneNode(boolean)">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>


    <item name="java.util.TimeZone java.util.TimeZone getTimeZone(java.lang.String)">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>


    <!-- BUG? - should be propagated -->
    <item name="java.security.MessageDigestSpi java.lang.Object clone()">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

conflict

    <!-- todo investigate - seems that all implementations returns `this` -->
    <item name="javax.swing.ListCellRenderer java.awt.Component getListCellRendererComponent(javax.swing.JList&lt;? extends E&gt;, E, int, boolean, boolean)">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>

see XPathNamespaceImpl

    <item name="org.w3c.dom.Node org.w3c.dom.Node insertBefore(org.w3c.dom.Node, org.w3c.dom.Node)">
        <annotation name="org.jetbrains.annotations.NotNull"/>
    </item>
