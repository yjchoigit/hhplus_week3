package com.hhplus.hhplus_week3_4_5.ecommerce.facade.order;

import com.hhplus.hhplus_week3_4_5.ecommerce.base.config.redis.RedisCustomException;
import com.hhplus.hhplus_week3_4_5.ecommerce.base.config.redis.RedisEnums;
import com.hhplus.hhplus_week3_4_5.ecommerce.controller.order.dto.CreateOrderApiReqDto;
import com.hhplus.hhplus_week3_4_5.ecommerce.controller.order.dto.FindOrderApiResDto;
import com.hhplus.hhplus_week3_4_5.ecommerce.infrastructure.apiClient.order.OrderCollectApiClient;
import com.hhplus.hhplus_week3_4_5.ecommerce.infrastructure.apiClient.order.dto.SendOrderToCollectionDto;
import com.hhplus.hhplus_week3_4_5.ecommerce.service.order.OrderPaymentService;
import com.hhplus.hhplus_week3_4_5.ecommerce.service.order.OrderService;
import com.hhplus.hhplus_week3_4_5.ecommerce.service.order.OrderSheetService;
import com.hhplus.hhplus_week3_4_5.ecommerce.service.point.PointService;
import com.hhplus.hhplus_week3_4_5.ecommerce.service.product.ProductService;
import com.hhplus.hhplus_week3_4_5.ecommerce.service.product.ProductStockService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@AllArgsConstructor
@Slf4j
public class OrderPaymentFacade {
    private OrderSheetService orderSheetService;
    private OrderService orderService;
    private PointService pointService;
    private ProductService productService;
    private ProductStockService productStockService;
    private OrderPaymentService orderPaymentService;
    private OrderCollectApiClient orderCollectApiClient;
    private RedissonClient redissonClient;

    // 주문 생성
    public Long createOrder(CreateOrderApiReqDto reqDto){
        // 주문서 id 기준으로 Lock 객체를 가져옴
        RLock rLock = redissonClient.getLock(RedisEnums.LockName.CREATE_ORDER.changeLockName(reqDto.orderSheetId()));
        boolean isLocked = false;

        try {
            // 락의 이름으로 RLock 인스턴스 가져옴
            log.info("try lock: {}", rLock.getName());
            isLocked = rLock.tryLock(0,  3, TimeUnit.SECONDS);

            // 락을 획득하지 못했을 떄
            if (!isLocked) {
                throw new RedisCustomException(RedisEnums.Error.LOCK_NOT_ACQUIRE);
            }

            // 상품 프로세스 진행 (상품 valid)
            productProcess(reqDto);
            // 재고 프로세스 진행 (재고 valid, 재고 차감처리)
            stockProcess(reqDto);

            // 주문 생성 진행
            Long orderId = orderService.createOrder(reqDto);
            // 주문 정보 조회
            FindOrderApiResDto orderInfo = orderService.findOrder(reqDto.buyerId(), orderId);

            // 주문서 삭제 처리
            orderSheetService.completeOrderSheet(reqDto.orderSheetId());

            return orderInfo.orderId();
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

    private void productProcess(CreateOrderApiReqDto reqDto){
        for(CreateOrderApiReqDto.CreateOrderItemApiReqDto item : reqDto.orderItemList()){
            Long productId = item.productId();
            Long productOptionId = item.productOptionId();
            
            // 상품 정보 조회
            productService.findProductByProductId(productId);
            
            // 상품 옵션 id 존재 시 상품 옵션 정보 조회
            if(productOptionId != null){
               productService.findProductOptionByProductIdAndProductOptionId(productId, productOptionId);
            }
        }
    }

    private void stockProcess(CreateOrderApiReqDto reqDto){
        for(CreateOrderApiReqDto.CreateOrderItemApiReqDto item : reqDto.orderItemList()) {
            Long productId = item.productId();
            Long productOptionId = item.productOptionId();
            int buyCnt = item.buyCnt();

            productStockService.deductProductStock(productId, productOptionId, buyCnt);
        }
    }


    // 결제 처리
    public Long paymentOrder(Long buyerId, Long orderId) {
        // 주문 id 기준으로 Lock 객체를 가져옴
        RLock rLock = redissonClient.getLock(RedisEnums.LockName.PAYMENT_ORDER.changeLockName(orderId));
        boolean isLocked = false;

        try {
            // 락의 이름으로 RLock 인스턴스 가져옴
            log.info("try lock: {}", rLock.getName());
            isLocked = rLock.tryLock(0,  3, TimeUnit.SECONDS);

            // 락을 획득하지 못했을 떄
            if (!isLocked) {
                throw new RedisCustomException(RedisEnums.Error.LOCK_NOT_ACQUIRE);
            }

            // 주문 정보 조회
            FindOrderApiResDto orderInfo = orderService.findOrder(buyerId, orderId);
            // 잔액 사용처리 (잔액 valid, 잔액 사용처리)
            pointService.usePoint(buyerId, orderInfo.totalPrice());
            // 결제 처리 -> orderPaymentId 반환
            Long orderPaymentId = orderPaymentService.paymentOrder(buyerId, orderId);

            // 주문 데이터 수집 외부 데이터 플랫폼 전달
            sendOrderToCollection(new SendOrderToCollectionDto(orderInfo.orderNumber(), orderInfo.totalPrice(), orderInfo.createDatetime()));

            return orderPaymentId;

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

    // 주문 데이터 수집 외부 데이터 플랫폼 전달
    private void sendOrderToCollection(SendOrderToCollectionDto sendDto){
        orderCollectApiClient.sendOrderToCollectionPlatform(sendDto);
    }
}
