package net.sephy.mybatis.util;

import org.springframework.dao.OptimisticLockingFailureException;

/**
 * @author Sephy
 * @since: 2015-07-30
 */
public abstract class OptimisticLockingUtils {

    /**
     * 检测乐观锁更新是否成功, 不成功抛出乐观锁异常
     * @param affectRows 影响行数
     * @throws {@link org.springframework.dao.OptimisticLockingFailureException}
     */
    public static final void checkOptimisticLockingFailure(int affectRows) {
        if (affectRows < 1) {
            throw new OptimisticLockingFailureException("Update failed, the data has been modified.");
        }
    }
}
