Apache OFBiz specific extension for the (very useful) unused code detector (http://www.ucdetector.org)
this extension is super useful after refactoring or long time customization where 
some of the services (and other items) are not used any more.

- mark methods that looks like a service but doesn't have a definition
- mark services (in the xml definition) that are not called from anywhere
- mark ftl files that are not included from screen definitions or include/import statement
- mark bsh/groovy files that are not included from screen definitions
- mark screens that are not referenced from views or rendered programmatically
- mark views that are calling not existing screens or not called from controllers

--- How to use -------------------------------------
just get the two jars from the build
and put it into eclipse's dropin directory
(In Ubuntu: ~/.eclipse/org.eclipse.platform../dropins)

--- How to modify ---------------------------------- 
you need to checkout first the org.ucdetector project from:
http://www.ucdetector.org/custom.html
and then follow the steps there. This extension follows the 
original plugin's coding style, lets respect that.

