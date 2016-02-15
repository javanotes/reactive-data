/* ============================================================================
*
* FILE: AbstractMembershipEventObserver.java
*
The MIT License (MIT)

Copyright (c) 2016 Sutanu Dalui

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*
* ============================================================================
*/
package com.reactivetechnologies.platform.datagrid.handlers;

import java.util.Observable;

import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;

public abstract class AbstractMembershipEventObserver
    implements MembershipEventObserver {

  @Override
  public final void update(Observable arg0, Object arg1) {
    
    if(arg1 instanceof MembershipEvent)
    {
      
      MembershipEvent me = (MembershipEvent) arg1;
      switch(me.getEventType())
      {
        case MembershipEvent.MEMBER_ADDED:
          handleMemberAdded(me.getMember());
          break;
        case MembershipEvent.MEMBER_REMOVED:
          handleMemberRemoved(me.getMember());
          break;
        case MembershipEvent.MEMBER_ATTRIBUTE_CHANGED:
          MemberAttributeEvent ma = (MemberAttributeEvent) arg1;
          handleMemberModified(ma.getMember(), ma.getOperationType());
          break;
          default: break;
      }
    }
    
  }

}
