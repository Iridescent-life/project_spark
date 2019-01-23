package org.qianfeng.qftfmp.utils.topk;

/**
 * @Auther: xinyu
 * @Date: 2019/1/22 14:31
 * @Description:
 */
public class MinHeap {
    int[] heap;
    int heapsize;

    public MinHeap(int[] array) {
        this.heap = array;
        this.heapsize = heap.length;
    }

    /**
     * 构建小顶堆
     */
    public void BuildMinHeap() {
        for (int i = heapsize / 2 - 1; i >= 0; i--) {
            Minify(i);// 依次向上将当前子树最大堆化
        }
    }

    /**
     * 堆排序
     */
    public void HeapSort() {
        for (int i = 0; i < heap.length; i++) {
            // 执行n次，将每个当前最大的值放到堆末尾
            swap(heap,0,heapsize-1);
            heapsize--;
            Minify(0);
        }
    }

    /**
     * 对非叶节点调整
     * @param i
     */
    public void Minify(int i) {
        int l = 2*i + 1;
        int r = 2*i + 2;
        int min;

        if (l < heapsize && heap[l] < heap[i])
            min = l;
        else
            min = i;
        if (r < heapsize && heap[r] < heap[min])
            min = r;
        if (min == i || min >= heapsize)// 如果largest等于i说明i是最大元素
            // largest超出heap范围说明不存在比i节点大的子女
            return;
        swap(heap,i,min);
        Minify(min);
    }

    private void swap(int[] heap, int i, int min) {
        int tmp = heap[i];// 交换i与largest对应的元素位置，在largest位置递归调用maxify
        heap[i] = heap[min];
        heap[min] = tmp;
    }

    public void IncreaseValue(int i, int val) {
        heap[i] = val;
        if (i >= heapsize || i <= 0 || heap[i] >= val)
            return;
        int p = Parent(i);
        if (heap[p] >= val)
            return;
        heap[i] = heap[p];
        IncreaseValue(p, val);
    }

    private int Parent(int i) {
        return (i - 1) / 2;
    }
}
