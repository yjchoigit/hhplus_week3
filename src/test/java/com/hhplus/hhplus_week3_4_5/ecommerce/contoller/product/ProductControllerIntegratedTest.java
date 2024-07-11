package com.hhplus.hhplus_week3_4_5.ecommerce.contoller.product;

import com.hhplus.hhplus_week3_4_5.ecommerce.contoller.Setting;
import com.hhplus.hhplus_week3_4_5.ecommerce.controller.product.dto.FindProductRankingApiResDto;
import com.hhplus.hhplus_week3_4_5.ecommerce.domain.buyer.entity.Buyer;
import com.hhplus.hhplus_week3_4_5.ecommerce.fixture.buyer.BuyerFixture;
import com.hhplus.hhplus_week3_4_5.ecommerce.fixture.order.OrderFixture;
import com.hhplus.hhplus_week3_4_5.ecommerce.fixture.product.ProductFixture;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ProductControllerIntegratedTest extends Setting {
    private static final String PATH = "/products";

    @Autowired
    private OrderFixture orderFixture;

    @Autowired
    private BuyerFixture buyerFixture;

    @Test
    @DisplayName("상위 상품 조회 성공")
    void findProductRanking_success(){
        // given
        Buyer buyer = buyerFixture.회원_등록();
        orderFixture.주문_등록(buyer, 10);
        orderFixture.주문_등록(buyer, 20);
        orderFixture.주문_등록(buyer, 30);
        orderFixture.주문_등록(buyer, 40);
        orderFixture.주문_등록(buyer, 50);
        orderFixture.주문_등록(buyer, 60);
        orderFixture.주문_등록(buyer, 70);

        // when
        ExtractableResponse<Response> response = get(PATH +"/ranking");

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());

        List<FindProductRankingApiResDto> list = response.jsonPath().getList(".", FindProductRankingApiResDto.class);
        assertThat(list).isNotEmpty();
    }
}
