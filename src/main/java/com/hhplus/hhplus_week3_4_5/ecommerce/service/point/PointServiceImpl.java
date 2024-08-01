package com.hhplus.hhplus_week3_4_5.ecommerce.service.point;

import com.hhplus.hhplus_week3_4_5.ecommerce.base.config.redis.RedisCustomException;
import com.hhplus.hhplus_week3_4_5.ecommerce.base.config.redis.RedisEnums;
import com.hhplus.hhplus_week3_4_5.ecommerce.controller.point.dto.FindPointHistoryApiResDto;
import com.hhplus.hhplus_week3_4_5.ecommerce.domain.point.PointEnums;
import com.hhplus.hhplus_week3_4_5.ecommerce.domain.point.entity.Point;
import com.hhplus.hhplus_week3_4_5.ecommerce.domain.point.entity.PointHistory;
import com.hhplus.hhplus_week3_4_5.ecommerce.domain.point.exception.PointCustomException;
import com.hhplus.hhplus_week3_4_5.ecommerce.domain.point.repository.PointHistoryRepository;
import com.hhplus.hhplus_week3_4_5.ecommerce.domain.point.repository.PointRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@AllArgsConstructor
@Transactional(rollbackFor = {Exception.class}, readOnly = true)
@Slf4j
public class PointServiceImpl implements PointService {
    private PointRepository pointRepository;
    private PointHistoryRepository pointHistoryRepository;
    private RedissonClient redissonClient;

    // 잔액 조회
    @Override
    @Transactional(rollbackFor = {Exception.class})
    public int findPoint(Long buyerId){
        // 회원 id로 잔액 정보 조회
        Point pointInfo = pointRepository.findByBuyerId(buyerId);
        if(pointInfo == null) {
            throw new PointCustomException(PointEnums.Error.NO_POINT);
        }
        // 총 포인트 반환
        return pointInfo.getAllPoint();
    }

    // 잔액 충전
    @Override
    @Transactional(rollbackFor = {Exception.class})
    public boolean chargePoint(Long buyerId, int point){
        // buyerId 기준으로 Lock 객체를 가져옴
        RLock rLock = redissonClient.getLock(RedisEnums.LockName.CHARGE_POINT.changeLockName(buyerId));
        boolean isLocked = false;

        try {
            // 락의 이름으로 RLock 인스턴스 가져옴
            log.info("try lock: {}", rLock.getName());
            isLocked = rLock.tryLock(0,  3, TimeUnit.SECONDS);

            // 락을 획득하지 못했을 떄
            if (!isLocked) {
                throw new RedisCustomException(RedisEnums.Error.LOCK_NOT_ACQUIRE);
            }

            // 회원 id로 잔액 정보 조회
            Point pointInfo = pointRepository.findByBuyerId(buyerId);
            // 잔액 충전
            pointInfo.charge(point);
            // 잔액 충전 내역 저장
            pointHistoryRepository.save(new PointHistory(pointInfo, PointEnums.Type.CHARGE, point));
            return true;

        } catch (InterruptedException e) {
            throw new RedisCustomException(RedisEnums.Error.LOCK_INTERRUPTED_ERROR);
        } finally {
            if (isLocked) {
                try{
                    if (rLock.isHeldByCurrentThread()) {
                        rLock.unlock();
                        log.info("unlock complete: {}", rLock.getName());
                    }
                }catch (IllegalMonitorStateException e){
                    //이미 종료된 락일 때 발생하는 예외
                    throw new RedisCustomException(RedisEnums.Error.UNLOCKING_A_LOCK_WHICH_IS_NOT_LOCKED);
                }
            }
        }
    }

    // 잔액 사용
    @Override
    @Transactional(rollbackFor = {Exception.class})
    public boolean usePoint(Long buyerId, int point) {
        // buyerId 기준으로 Lock 객체를 가져옴
        RLock rLock = redissonClient.getLock(RedisEnums.LockName.USE_POINT.changeLockName(buyerId));
        boolean isLocked = false;

        try {
            // 락의 이름으로 RLock 인스턴스 가져옴
            log.info("try lock: {}", rLock.getName());
            isLocked = rLock.tryLock(0,  3, TimeUnit.SECONDS);

            // 락을 획득하지 못했을 떄
            if (!isLocked) {
                throw new RedisCustomException(RedisEnums.Error.LOCK_NOT_ACQUIRE);
            }

            // 회원 id로 잔액 정보 조회
            Point pointInfo = pointRepository.findByBuyerId(buyerId);
            // 잔액 사용
            pointInfo.use(point);
            // 잔액 사용 내역 저장
            pointHistoryRepository.save(new PointHistory(pointInfo, PointEnums.Type.DEDUCT, point));
            return true;

        } catch (InterruptedException e) {
            throw new RedisCustomException(RedisEnums.Error.LOCK_INTERRUPTED_ERROR);
        } finally {
            if (isLocked) {
                try{
                    if (rLock.isHeldByCurrentThread()) {
                        rLock.unlock();
                        log.info("unlock complete: {}", rLock.getName());
                    }
                }catch (IllegalMonitorStateException e){
                    //이미 종료된 락일 때 발생하는 예외
                    throw new RedisCustomException(RedisEnums.Error.UNLOCKING_A_LOCK_WHICH_IS_NOT_LOCKED);
                }
            }
        }
    }

    @Override
    public List<FindPointHistoryApiResDto> findPointHistoryList(Long buyerId) {
        // 회원 id로 잔액 정보 조회
        Point pointInfo = pointRepository.findByBuyerId(buyerId);
        // 잔액 id로 내역 조회
        List<PointHistory> pointHistoryList = pointHistoryRepository.findByPointId(pointInfo.getPointId());
        if(pointHistoryList.isEmpty()){
            return new ArrayList<>();
        }
        return pointHistoryList.stream().map(FindPointHistoryApiResDto::from).toList();
    }
}
