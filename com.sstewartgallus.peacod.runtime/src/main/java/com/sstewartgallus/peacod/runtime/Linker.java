package com.sstewartgallus.peacod.runtime;

import jdk.dynalink.NamedOperation;
import jdk.dynalink.NamespaceOperation;
import jdk.dynalink.linker.GuardedInvocation;
import jdk.dynalink.linker.LinkRequest;
import jdk.dynalink.linker.LinkerServices;
import jdk.dynalink.linker.TypeBasedGuardingDynamicLinker;

import java.util.Arrays;

class Linker implements TypeBasedGuardingDynamicLinker {


    @Override
    public GuardedInvocation getGuardedInvocation(LinkRequest linkRequest, LinkerServices linkerServices) {
        var receiver = (Context) linkRequest.getReceiver();
        var callSiteDescriptor = linkRequest.getCallSiteDescriptor();
        var op = callSiteDescriptor.getOperation();

        Object name = null;
        if (op instanceof NamedOperation) {
            name = ((NamedOperation) op).getName();
            op = ((NamedOperation) op).getBaseOperation();
        }

        Object[] namespaces = {};
        if (op instanceof NamespaceOperation) {
            namespaces = ((NamespaceOperation) op).getNamespaces();
            op = ((NamespaceOperation) op).getBaseOperation();
        }

        var args = linkRequest.getArguments();
        var methodType = callSiteDescriptor.getMethodType();

        return receiver.link(op, namespaces, name, methodType, receiver, args);
    }

    @Override
    public boolean canLinkType(Class<?> type) {
        return Context.class.isAssignableFrom(type);
    }
}
