package org.qianfeng.qftfmp.utils;

/**
 * @Auther: xinyu
 * @Date: 2019/1/22 15:58
 * @Description:
 */
class ListNode {
    int val;
    ListNode next;
    ListNode(int x) {
        val = x;
    }
}

public class Solution {
    public boolean isPalindrome(ListNode head) {
        if(head == null || head.next == null){
            return true;
        }

        ListNode reverseHead = new ListNode(head.val);
        reverseHead.next = null;
        ListNode curr = head.next;
        ListNode nextNode;

        int count = 0;
        while (head != null){
            count++;
            head = head.next;
        }

        int n = count/2;
        while (--n > 0){
            nextNode = curr.next;
            curr.next = reverseHead;
            reverseHead = curr;
            curr = nextNode;
        }

        if(count%2 == 1){
            curr = curr.next;
        }

        while (curr != null){
            if(curr.val != reverseHead.val){
                return false;
            }
            curr = curr.next;
            reverseHead = reverseHead.next;
        }

        return true;
    }
    public static void main(String[] args){
        ListNode head = new ListNode(1);
        ListNode one = new ListNode(2);
        ListNode two = new ListNode(3);
        ListNode three = new ListNode(2);
        ListNode four = new ListNode(4);
        head.next = one;
        one.next = two;
        two.next = three;
        three.next = four;
        four.next = null;
        System.out.println(new Solution().isPalindrome(head));
    }
}
