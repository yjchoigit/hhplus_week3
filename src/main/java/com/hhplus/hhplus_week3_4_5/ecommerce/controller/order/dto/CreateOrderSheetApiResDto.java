package com.hhplus.hhplus_week3_4_5.ecommerce.controller.order.dto;

import com.hhplus.hhplus_week3_4_5.ecommerce.domain.order.entity.OrderItemSheet;
import com.hhplus.hhplus_week3_4_5.ecommerce.domain.order.entity.OrderSheet;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;

public record CreateOrderSheetApiResDto(
    @Schema(description = "주문서 ID")
    Long orderSheetId,
    @Schema(description = "회원 ID")
    Long buyerId,
    @Schema(description = "주문자명")
    String buyerName,
    @Schema(description = "총 구매수량")
    int allBuyCnt,
    @Schema(description = "총 상품 가격")
    int totalPrice,
    @Schema(description = "주문 품목 리스트")
    List<CreateOrderItemSheetApiResDto> orderItemList
) implements Serializable {

    public static CreateOrderSheetApiResDto from(OrderSheet orderSheet, List<CreateOrderItemSheetApiResDto> orderItemList){
        return new CreateOrderSheetApiResDto(orderSheet.getOrderSheetId(), orderSheet.getBuyerId(), orderSheet.getBuyerName(),
                orderSheet.getAllBuyCnt(), orderSheet.getTotalPrice(), orderItemList);
    }

    public record CreateOrderItemSheetApiResDto(
            @Schema(description = "주문서 품목 ID")
            Long orderSheetItemId,
            @Schema(description = "상품 ID")
            Long productId,
            @Schema(description = "상품명")
            String productName,
            @Schema(description = "상품 옵션 ID")
            Long productOptionId,
            @Schema(description = "상품 옵션명")
            String productOptionName,
            @Schema(description = "상품 가격")
            int productPrice,
            @Schema(description = "상품 구매수량")
            int buyCnt
    ) implements Serializable {

        public static CreateOrderItemSheetApiResDto from(OrderItemSheet orderItemSheet){
            return new CreateOrderItemSheetApiResDto(orderItemSheet.getOrderItemSheetId(), orderItemSheet.getProductId(),
                    orderItemSheet.getProductName(), orderItemSheet.getProductOptionId(), orderItemSheet.getProductOptionName(),
                    orderItemSheet.getProductPrice(), orderItemSheet.getBuyCnt());
        }
    }

}
