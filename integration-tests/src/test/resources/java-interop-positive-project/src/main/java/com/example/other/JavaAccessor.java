package com.example.other;

import com.example.internal.MixedVisibility;
import com.example.internal.FullyPublic;

// This Java class accesses public members from a different package
// Should compile successfully
public class JavaAccessor {
    public String accessPublicMembers() {
        MixedVisibility mixed = new MixedVisibility();
        // Can access public method
        String publicResult = mixed.publicMethod();
        // Can access public property
        String publicProp = mixed.getPublicProperty();
        
        FullyPublic fullyPublic = new FullyPublic();
        // Can access all members of non-package-private class
        String accessible = fullyPublic.accessibleMethod();
        String accessibleProp = fullyPublic.getAccessibleProperty();
        
        return publicResult + publicProp + accessible + accessibleProp;
    }
}
