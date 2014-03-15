Ofbiz specific extension for unused code detector

- mark methods that looks like a service but doesnt have a definition
- mark services (in the xml definition) that are not called from anywhere
- mark ftl files that are not included from screen definitions or include/import statement
- mark bsh/groovy files that are not included from screen definitions

--- To modify ---------------------------------- 
you need to checkout first the org.ucdetector project from:
http://www.ucdetector.org/custom.html
and then follow the steps there.

--- To use -------------------------------------
just get the two jars from the build
and put it into eclipse's dropin directory
(In Ubuntu: ~/.eclipse/org.eclipse.platform../dropins)