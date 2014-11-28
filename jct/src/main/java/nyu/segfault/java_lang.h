#pragma once

#include <stdint.h>
#include <string>
#include <vector>

// Forward declarations of data layout and vtables.
struct __Object;
struct __Object_VT;

struct __String;
struct __String_VT;

struct __Class;
struct __Class_VT;

// Definition of types that are equivalent to Java semantics,
// i.e., an instance is the address of the object's data layout.
typedef __Object* Object;
typedef __Class* Class;
typedef __String* String;

// ======================================================================

// The data layout for java.lang.Object.
struct __Object {
  __Object_VT* __vptr;

  // The constructor.
  __Object();

  // The methods implemented by java.lang.Object.
  static int32_t hashCode(Object);
  static bool equals(Object, Object);
  static Class getClass(Object);
  static String toString(Object);

  // The function returning the class object representing
  // java.lang.Object.
  static Class __class();

  // The vtable for java.lang.Object.
  static __Object_VT __vtable;
};

// The vtable layout for java.lang.Object.
struct __Object_VT {
  Class __isa;
  int32_t (*hashCode)(Object);
  bool (*equals)(Object, Object);
  Class (*getClass)(Object);
  String (*toString)(Object);

  __Object_VT()
  : __isa(__Object::__class()),
    hashCode(&__Object::hashCode),
    equals(&__Object::equals),
    getClass(&__Object::getClass),
    toString(&__Object::toString) {
  }
};

// ======================================================================

// The data layout for java.lang.String.
struct __String {
  __String_VT* __vptr;
  std::string data;

  // The constructor;
  __String(std::string data);

  // The methods implemented by java.lang.String.
  static int32_t hashCode(String);
  static bool equals(String, Object);
  static String toString(String);
  static int32_t length(String);
  static char charAt(String, int32_t);

  // The function returning the class object representing
  // java.lang.String.
  static Class __class();

  // The vtable for java.lang.String.
  static __String_VT __vtable;
};

// The vtable layout for java.lang.String.
struct __String_VT {
  Class __isa;
  int32_t (*hashCode)(String);
  bool (*equals)(String, Object);
  Class (*getClass)(String);
  String (*toString)(String);
  int32_t (*length)(String);
  char (*charAt)(String, int32_t);

  __String_VT()
  : __isa(__String::__class()),
    hashCode(&__String::hashCode),
    equals(&__String::equals),
    getClass((Class(*)(String)) &__Object::getClass),
    toString(&__String::toString),
    length(&__String::length),
    charAt(&__String::charAt) {
  }
};

// ======================================================================

// The data layout for java.lang.Class.
struct __Class {
  __Class_VT* __vptr;
  String name;
  Class parent;

  // The constructor.
  __Class(String name, Class parent);

  // The instance methods of java.lang.Class.
  static String toString(Class);
  static String getName(Class);
  static Class getSuperclass(Class);
  static bool isInstance(Class, Object);

  // The function returning the class object representing
  // java.lang.Class.
  static Class __class();

  // The vtable for java.lang.Class.
  static __Class_VT __vtable;
};

// The vtable layout for java.lang.Class.
struct __Class_VT {
  Class __isa;
  int32_t (*hashCode)(Class);
  bool (*equals)(Class, Object);
  Class (*getClass)(Class);
  String (*toString)(Class);
  String (*getName)(Class);
  Class (*getSuperclass)(Class);
  bool (*isInstance)(Class, Object);

  __Class_VT()
  : __isa(__Class::__class()),
    hashCode((int32_t(*)(Class)) &__Object::hashCode),
    equals((bool(*)(Class,Object)) &__Object::equals),
    getClass((Class(*)(Class)) &__Object::getClass),
    toString(&__Class::toString),
    getName(&__Class::getName),
    getSuperclass(&__Class::getSuperclass),
    isInstance(&__Class::isInstance) {
  }
};






/*******************   IMPLEMENTATIONS   **********************/

__Object __Object::__Object() {

}

/* Object implementation. */
int32_t __Object::hashCode(Object o) {
    return o;
}

bool __Object::equals(Object o1, Object o2) {
    return o1 == o2 ? true : false;
}

Class __Object::getClass(Object o) {
    return o::_class();
}

String __Object::toString(Object o) {
    int32_t hash = __Object.hashCode(o);
    std::string hashStr (to_string(hash));
    return hashStr;
}


/* String implementation. */
bool __String::equals(String s, Object o) {
    std::string str1 (s->data);
    std::string str2 (o->toString());
    return str1.compare(str2) == 0 ? true : false;
}

String __String::toString(String s) {
    return s->data;
}

int32_t __String::length(String s) {
    return s->data->length();
}

char __String::charAt(String s, int32_t index) {
    return s->data->at(index);
}


/* Class implementation. */
String __Class::toString(Class c) {
    return c->name;
}

String __Class::getName(Class c) {
    return c->name;
}

Class __Class::getSuperclass(Class c) {
    return c->parent;
}

bool __Class::isInstance(Class c, Object o) {
    Class *tempClass = __Object.getClass(o);
    while (!(*tempClass == NULL)  &&  !c->getClass()->equals(tempClass->getClass())) {
        tempClass = tempClass->parent;
    }
    return *tempClass == NULL ? false : true;
}
