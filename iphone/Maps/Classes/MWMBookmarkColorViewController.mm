//
//  MWMBookmarkColorViewController.m
//  Maps
//
//  Created by v.mikhaylenko on 27.05.15.
//  Copyright (c) 2015 MapsWithMe. All rights reserved.
//

#import "MWMBookmarkColorViewController.h"
#import "UIKitCategories.h"
#import "MWMBookmarkColorCell.h"
#import "MWMPlacePageEntity.h"
#import "MWMPlacePageViewManager.h"

@interface MWMTableView : UITableView

@end

@implementation MWMTableView

//- (void)setContentInset:(UIEdgeInsets)contentInset
//{
//// Workaround on apple "feature" with navigationController (see NavigationController.mm, line 22).
//  return;
//  [super setContentInset:UIEdgeInsetsZero];
//}
////
//- (void)setContentOffset:(CGPoint)contentOffset
//{
//// Workaround on apple "feature" with navigationController (see NavigationController.mm, line 22).
//  [super setContentOffset:CGPointZero];
//}
////
//- (void)setScrollEnabled:(BOOL)scrollEnabled
//{
//  [super setScrollEnabled:YES];
//}
@end

extern NSArray * const kBookmarkColorsVariant;

static NSString * const kBookmarkColorCellIdentifier = @"MWMBookmarkColorCell";

@interface MWMBookmarkColorViewController ()

@property (weak, nonatomic) IBOutlet UITableView * tableView;
@property (nonatomic) CGFloat realPlacePageHeight;

@end

@interface MWMBookmarkColorViewController (TableView) <UITableViewDataSource, UITableViewDelegate>
@end

@implementation MWMBookmarkColorViewController

- (void)viewDidLoad
{
  [super viewDidLoad];
  [self.ownerNavigationController setNavigationBarHidden:NO];
  self.title = L(@"bookmark_color");
  [self.tableView registerNib:[UINib nibWithNibName:kBookmarkColorCellIdentifier bundle:nil] forCellReuseIdentifier:kBookmarkColorCellIdentifier];
}

- (void)viewWillAppear:(BOOL)animated
{
  [super viewWillAppear:animated];
  [self configureTableViewForOrientation:self.interfaceOrientation];
  [self.tableView reloadData];

  if (!self.ownerNavigationController)
    return;

  self.realPlacePageHeight = self.ownerNavigationController.view.height;
  CGFloat const bottomOffset = 88.;
  self.ownerNavigationController.view.height = self.tableView.height + bottomOffset;
  UIImage * backImage = [UIImage imageNamed:@"NavigationBarBackButton"];
  UIButton * backButton = [[UIButton alloc] initWithFrame:CGRectMake(0., 0., backImage.size.width, backImage.size.height)];
  [backButton addTarget:self action:@selector(backTap:) forControlEvents:UIControlEventTouchUpInside];
  [backButton setImage:backImage forState:UIControlStateNormal];
  UIBarButtonItem * leftButton = [[UIBarButtonItem alloc] initWithCustomView:backButton];
  [self.navigationItem setLeftBarButtonItem:leftButton];
}

- (void)viewDidAppear:(BOOL)animated
{
  [super viewDidAppear:animated];
}

- (void)backTap:(id)sender
{
  [self.navigationController popViewControllerAnimated:YES];
}

- (void)configureTableViewForOrientation:(UIInterfaceOrientation)orientation
{
  if (self.ownerNavigationController)
    return;

  CGFloat const defaultHeight = 352.;
  CGSize size = self.navigationController.view.bounds.size;
  CGFloat width, height;

  switch (orientation)
  {
    case UIInterfaceOrientationUnknown:
      break;

    case UIInterfaceOrientationPortraitUpsideDown:
    case UIInterfaceOrientationPortrait:
    {
      CGFloat const topOffset = 88.;
      width = size.width < size.height ? size.width : size.height;
      height = size.width > size.height ? size.width : size.height;
      CGFloat const externalHeight = self.navigationController.navigationBar.height + [[UIApplication sharedApplication] statusBarFrame].size.height;
      CGFloat const actualHeight = defaultHeight > (height - externalHeight) ? height : defaultHeight;
      self.tableView.frame = CGRectMake(0., topOffset, width, actualHeight);
//      self.tableView.contentInset = UIEdgeInsetsZero;
      break;
    }

    case UIInterfaceOrientationLandscapeLeft:
    case UIInterfaceOrientationLandscapeRight:
    {
      CGFloat const navBarHeight = self.navigationController.navigationBar.height;
      width = size.width > size.height ? size.width : size.height;
      height = size.width < size.height ? size.width : size.height;
      CGFloat const currentHeight = height - navBarHeight;
      CGFloat const actualHeight = currentHeight > defaultHeight ? defaultHeight : currentHeight;
      self.tableView.frame = CGRectMake(0., navBarHeight, width, actualHeight);
//      self.tableView.contentInset = UIEdgeInsetsZero;
      break;
    }
  }
}

- (void)didRotateFromInterfaceOrientation:(UIInterfaceOrientation)fromInterfaceOrientation
{
  [self configureTableViewForOrientation:self.interfaceOrientation];
}

- (BOOL)shouldAutorotate
{
  return YES;
}

- (void)viewWillDisappear:(BOOL)animated
{
  [super viewWillDisappear:animated];
  [self.placePageManager reloadBookmark];

  if (!self.ownerNavigationController)
    return;

  self.ownerNavigationController.navigationBar.hidden = YES;
  [self.ownerNavigationController setNavigationBarHidden:YES];
  self.ownerNavigationController.view.height = self.realPlacePageHeight;
}

@end

@implementation MWMBookmarkColorViewController (TableView)

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
  MWMBookmarkColorCell * cell = (MWMBookmarkColorCell *)[tableView dequeueReusableCellWithIdentifier:kBookmarkColorCellIdentifier];
  if (!cell)
    cell = [[MWMBookmarkColorCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:kBookmarkColorCellIdentifier];

  NSString * const currentColor = kBookmarkColorsVariant[indexPath.row];
  [cell configureWithColorString:kBookmarkColorsVariant[indexPath.row]];

  if ([currentColor isEqualToString:self.placePageManager.entity.bookmarkColor] && !cell.selected)
    [tableView selectRowAtIndexPath:indexPath animated:NO scrollPosition:UITableViewScrollPositionNone];

  return cell;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
  return kBookmarkColorsVariant.count;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
  self.placePageManager.entity.bookmarkColor = kBookmarkColorsVariant[indexPath.row];
}

@end
