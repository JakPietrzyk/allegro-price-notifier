import pytest
import main
from unittest.mock import patch, Mock

SEARCH_HTML = """
<div class="cat-prod-row">
    <a class="js_seo-url" href="/123456">Link do produktu</a>
</div>
"""

PRODUCT_HTML = """
<h1 class="product-top__product-info__name">Konsola Testowa</h1>
<div class="product-offers__list">
    <div class="product-offer__container">
        <span class="price"><span class="value">3000</span></span>
        <div class="product-offer__details">
            <img class="store-logo" alt="Drogi Sklep" />
        </div>
    </div>
    <div class="product-offer__container">
        <span class="price">
            <span class="value">2 499</span><span class="penny">,99</span>
        </span>
        <div class="product-offer__details">
            <img class="store-logo" alt="MediaExpert" />
        </div>
    </div>
</div>
"""


@pytest.fixture
def client():
    main.app.config['TESTING'] = True
    with main.app.test_client() as client:
        yield client


@patch('main.requests.get')
def test_find_price_flow(mock_get, client):
    mock_search_resp = Mock()
    mock_search_resp.status_code = 200
    mock_search_resp.content = SEARCH_HTML

    mock_product_resp = Mock()
    mock_product_resp.status_code = 200
    mock_product_resp.content = PRODUCT_HTML

    mock_get.side_effect = [mock_search_resp, mock_product_resp]

    response = client.post('/find_price', json={'productName': 'Konsola'})

    assert response.status_code == 200
    data = response.get_json()

    assert data['search_query'] == 'Konsola'
    assert data['found_product_name'] == 'Konsola Testowa'
    assert data['price'] == 2499.99
    assert data['shop_name'] == 'MediaExpert'
    assert "ceneo.pl/123456" in data['ceneo_url']


@patch('main.requests.get')
def test_product_not_found(mock_get, client):
    mock_resp = Mock()
    mock_resp.status_code = 200
    mock_resp.content = "<html><body>No results</body></html>"
    mock_get.return_value = mock_resp

    response = client.post('/find_price', json={'productName': 'invalid'})

    assert response.status_code == 404