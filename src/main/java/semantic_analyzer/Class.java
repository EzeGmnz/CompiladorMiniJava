package semantic_analyzer;

import exceptions.SemanticException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Class extends IClass {

    private final Collection<IClassType> interfaceInheritanceList;
    private final Map<String, IVariable> attributeMap, inheritedWithSameNameAttributes;
    private final Map<String, IMethod> methodMap;
    private IClassType parentClassRef;
    private IMethod constructor;
    private IType genericType;
    private boolean didConsolidate;

    public Class(String name) {
        this(name, "", 0, 0);
    }

    public Class(String name, String line, int row, int column) {
        super(name, line, row, column);
        attributeMap = new HashMap<>();
        inheritedWithSameNameAttributes = new HashMap<>();
        methodMap = new HashMap<>();
        interfaceInheritanceList = new ArrayList<>();
        didConsolidate = false;
    }

    @Override
    public Map<String, IVariable> getInheritedWithSameNameAttributes() {
        return inheritedWithSameNameAttributes;
    }

    @Override
    public IClassType getParentClassRef() {
        return parentClassRef;
    }

    @Override
    public void setParentClassRef(IClassType iClass) {
        parentClassRef = iClass;
    }

    @Override
    public Collection<IClassType> getInterfaceInheritanceList() {
        return interfaceInheritanceList;
    }

    @Override
    public Map<String, IVariable> getAttributeMap() {
        return attributeMap;
    }

    @Override
    public Map<String, IMethod> getMethodMap() {
        return methodMap;
    }

    @Override
    public IMethod getConstructor() {
        return constructor;
    }

    @Override
    public void setConstructor(IMethod constructor) {
        this.constructor = constructor;
    }

    @Override
    public IType getGenericType() {
        return genericType;
    }

    @Override
    public void setGenericType(IClassType genericClassRef) {
        this.genericType = genericClassRef;
    }

    @Override
    public void addInterfaceInheritance(IClassType iInterface) {
        interfaceInheritanceList.add(iInterface);
    }

    @Override
    public void addAttribute(IVariable attribute) {
        attributeMap.put(attribute.getName(), attribute);
    }

    @Override
    public void addMethod(IMethod method) {
        methodMap.put(method.getName(), method);
    }

    @Override
    public boolean containsAttribute(String name) {
        return attributeMap.containsKey(name);
    }

    @Override
    public boolean containsMethod(String name) {
        return methodMap.containsKey(name);
    }

    @Override
    public boolean containsInterfaceInheritance(String name) {
        for (IClassType c : interfaceInheritanceList) {
            if (c.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean hasAncestor(String name) throws SemanticException {
        if (getName().equals(name)) {
            return true;
        }
        if (parentClassRef == null) {
            return false;
        }

        IClass parentClass = SymbolTable.getInstance().getClass(this.parentClassRef.getName());
        if (parentClass != null) {
            return parentClass.hasAncestor(name);
        }

        return false;
    }

    @Override
    public void compareTo(Object o) {

    }

    @Override
    public void consolidate() throws SemanticException {
        if (!didConsolidate) {
            didConsolidate = true;
            genericTypeCheck();
            validateClassInheritance();
            validateInterfaceInheritance();
            consolidateAncestors();
            addConstructorIfMissing();

            addInheritedMethodsFromParentClass();
            addInheritedMethodsFromInterfaces();

            addInheritedAttributesFromParentClass();
            validateMethods();
            validateAttributes();
        }
    }

    private void genericTypeCheck() throws SemanticException {
        if (genericType != null) {
            IInterface inter = SymbolTable.getInstance().getInterface(genericType.getName());
            IClass c = SymbolTable.getInstance().getClass(genericType.getName());
            if (inter != null) {
                throw new SemanticException(genericType, "El tipo genrico no puede ser una interfaz");
            }
            if (c != null) {
                throw new SemanticException(genericType, "El tipo generico no puede ser una clase");
            }
        }
    }

    private void consolidateAncestors() throws SemanticException {
        if (parentClassRef != null) {
            IClass parentClass = SymbolTable.getInstance().getClass(this.parentClassRef.getName());
            parentClass.consolidate();
        }

        for (IClassType c : interfaceInheritanceList) {
            IInterface iInterface = getInterfaceForReference(c);
            iInterface.consolidate();
        }
    }

    private void validateClassInheritance() throws SemanticException {
        if (parentClassRef != null) {
            IClass classForParent = getClassForReference(parentClassRef);
            if (classForParent == null) {
                if (getInterfaceForReference(parentClassRef) != null) {
                    throw new SemanticException(parentClassRef, parentClassRef.getName() + " es una interfaz");
                }
                throw new SemanticException(parentClassRef, parentClassRef.getName() + " clase no definida");
            }
            parentClassRef.validate(genericType);
            if (classForParent.hasAncestor(this.getName())) {
                throw new SemanticException(this, "La clase sufre de herencia circular");
            }
        }
    }

    private void validateInterfaceInheritance() throws SemanticException {
        for (IClassType interfaceRef : interfaceInheritanceList) {
            IInterface parentInterface = getInterfaceForReference(interfaceRef);
            if (parentInterface == null) {
                if (getClassForReference(interfaceRef) != null) {
                    throw new SemanticException(interfaceRef, interfaceRef.getName() + " es una clase");
                }
                throw new SemanticException(interfaceRef, "interfaz no definida");
            }
            interfaceRef.validate(genericType);
            if (parentInterface.hasAncestor(this.getName())) {
                throw new SemanticException(this, "La clase sufre de herencia circular");
            }
        }
    }

    private void addConstructorIfMissing() {
        if (constructor == null) {
            IClassType classType = new TypeClass(getName());
            constructor = new Constructor(getName(), classType);
        }
    }

    private void addInheritedMethodsFromParentClass() throws SemanticException {
        if (parentClassRef != null) {
            IClass iClass = getClassForReference(parentClassRef);
            addMethodsFromClass(iClass);
        }
    }

    private void addMethodsFromClass(IClass iClass) throws SemanticException {
        IMethod methodWithSameName;
        for (IMethod inheritedMethod : iClass.getMethodMap().values()) {
            if (!inheritedMethod.getName().equals("main")) {
                methodWithSameName = methodMap.get(inheritedMethod.getName());
                if (methodWithSameName != null) {
                    methodWithSameName.validateOverwrite(parentClassRef, inheritedMethod);
                } else {
                    int columnFix = getColumn() + inheritedMethod.getName().length() + getName().length();
                    methodMap.put(
                            inheritedMethod.getName(),
                            inheritedMethod.cloneForOverwrite(getLine(), getRow(), columnFix)
                    );
                }
            }
        }
    }

    private IInterface getInterfaceForReference(IClassType c) {
        return SymbolTable.getInstance().getInterface(c.getName());
    }

    private IClass getClassForReference(IClassType c) {
        return SymbolTable.getInstance().getClass(c.getName());
    }

    private void addInheritedMethodsFromInterfaces() throws SemanticException {
        for (IClassType c : interfaceInheritanceList) {
            addMethodsFromInterface(c);
        }
    }

    private void addMethodsFromInterface(IClassType interfaceRef) throws SemanticException {
        IMethod methodWithSameName;
        IInterface iInterface = getInterfaceForReference(interfaceRef);
        for (IMethod inheritedMethod : iInterface.getMethodMap().values()) {
            methodWithSameName = methodMap.get(inheritedMethod.getName());
            if (methodWithSameName != null) {
                methodWithSameName.validateOverwrite(interfaceRef, inheritedMethod);
            } else {
                SemanticException methodNotImplException = new SemanticException(this,
                        "la clase " + getName() + " no implementa el metodo "
                                + inheritedMethod.getName() +
                                " de la interfaz " +
                                iInterface.getName());

                SymbolTable.getInstance().saveException(methodNotImplException);
            }
        }
    }

    private void addInheritedAttributesFromParentClass() {
        if (parentClassRef != null) {
            IClass parentClass = SymbolTable.getInstance().getClass(parentClassRef.getName());
            for (IVariable v : parentClass.getAttributeMap().values()) {
                IVariable variableWithSameName = attributeMap.get(v.getName());
                IVariable vClone = v.cloneForOverwrite(parentClassRef);
                if (variableWithSameName != null) {
                    inheritedWithSameNameAttributes.put(v.getName(), vClone);
                } else {
                    attributeMap.put(v.getName(), vClone);
                }
            }
        }
    }

    private void validateMethods() {
        for (IMethod m : methodMap.values()) {
            try {
                m.validate(genericType);
            } catch (SemanticException e) {
                SymbolTable.getInstance().saveException(e);
            }
        }
        try {
            constructor.validate(genericType);
        } catch (SemanticException e) {
            SymbolTable.getInstance().saveException(e);
        }
    }

    private void validateAttributes() throws SemanticException {
        for (IVariable v : attributeMap.values()) {
            v.validate(genericType);
        }
    }

}
