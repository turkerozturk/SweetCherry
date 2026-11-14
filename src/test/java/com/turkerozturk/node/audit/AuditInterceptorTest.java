package com.turkerozturk.node.audit;
import org.junit.jupiter.api.Test;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.transaction.PlatformTransactionManager;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class AuditInterceptorTest {
 @Test void disabledLoggingDoesNotReadTheDatabaseOrStartAnotherTransaction()throws Throwable{
  var transaction=mock(PlatformTransactionManager.class);var call=mock(MethodInvocation.class);when(call.proceed()).thenReturn("unchanged");var audit=new AuditInterceptor(transaction,false,16777216);assertThat(audit.invoke(call)).isEqualTo("unchanged");verify(call).proceed();verifyNoMoreInteractions(call);verifyNoInteractions(transaction);
 }
 @Test void invalidSizeLimitsAreRejected(){assertThatThrownBy(()->new AuditInterceptor(mock(PlatformTransactionManager.class),true,0)).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->new AuditInterceptor(mock(PlatformTransactionManager.class),true,67108865)).isInstanceOf(IllegalArgumentException.class);}
}
