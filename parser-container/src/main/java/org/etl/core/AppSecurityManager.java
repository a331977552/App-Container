package org.etl.core;

import java.security.Permission;

public class AppSecurityManager extends SecurityManager {

    @Override
    public void checkExit(int status) {
        System.out.println("exiting: "+ status );
    }

    @Override
    public void checkPermission(Permission perm) {
        // Allow other activities by default
        if( "exitVM".equals( perm.getName() ) ) {
            System.out.println("123");
            throw new ExitTrappedException() ;
        }
    }

    private static class ExitTrappedException extends SecurityException { }

    public static void forbidSystemExitCall() {
       System.setSecurityManager(new AppSecurityManager());
    }

    public static void enableSystemExitCall() {
        System.setSecurityManager( null ) ;
    }
}