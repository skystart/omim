//
//  MWMiPadPlacePageView.m
//  Maps
//
//  Created by v.mikhaylenko on 18.05.15.
//  Copyright (c) 2015 MapsWithMe. All rights reserved.
//

#import "MWMiPadPlacePage.h"
#import "MWMPlacePageViewManager.h"
#import "MWMPlacePageActionBar.h"
#import "UIKitCategories.h"
#import "MWMBasePlacePageView.h"
#import "MWMBookmarkColorViewController.h"
#import "SelectSetVC.h"
#import "UIViewController+Navigation.h"
#import "MWMBookmarkDescriptionViewController.h"

@interface MWMNavigationController : UINavigationController

@end

@implementation MWMNavigationController

- (instancetype)initWithRootViewController:(UIViewController *)rootViewController
{
  self = [super initWithRootViewController:rootViewController];
  if (self)
  {
    [self setNavigationBarHidden:YES];
    self.view.autoresizingMask = UIViewAutoresizingNone;
  }
  return self;
}

- (void)backTap:(id)sender
{
  [self popViewControllerAnimated:YES];
}

@end

@interface MWMPlacePageViewController : UIViewController

@end

@implementation MWMPlacePageViewController

- (void)viewWillAppear:(BOOL)animated
{
  [super viewWillAppear:animated];
  [self.navigationController setNavigationBarHidden:YES];
  self.view.autoresizingMask = UIViewAutoresizingNone;
}

@end

extern CGFloat kBookmarkCellHeight;

@interface MWMiPadPlacePage ()

@property (strong, nonatomic) MWMNavigationController * navigationController;
@property (strong, nonatomic) MWMPlacePageViewController * viewController;

@end

@implementation MWMiPadPlacePage

- (void)configure
{
  [super configure];
  UIView const * view = self.manager.ownerViewController.view;

  self.viewController = [[MWMPlacePageViewController alloc] init];
  [self.navigationController.view removeFromSuperview];
  [self.navigationController removeFromParentViewController];
  self.navigationController = [[MWMNavigationController alloc] initWithRootViewController:self.viewController];


  CGFloat const topOffset = 36.;
  CGFloat const leftOffset = 12.;
  CGFloat const defaultWidth = 360.;

  CGFloat const kActionBarHeight = 58.;

  CGFloat const defaultHeight = self.basePlacePageView.height + self.anchorImageView.height + kActionBarHeight;
  [self.manager.ownerViewController addChildViewController:self.navigationController];

  self.navigationController.view.frame = CGRectMake(leftOffset, topOffset, defaultWidth, defaultHeight);
  self.viewController.view.frame = CGRectMake(leftOffset, topOffset, defaultWidth, defaultHeight);

  self.extendedPlacePageView.frame = CGRectMake(0., 0., defaultWidth, defaultHeight - 1);
  self.anchorImageView.image = nil;
  self.anchorImageView.backgroundColor = [UIColor whiteColor];

  self.actionBar.width = defaultWidth;
  self.actionBar.origin = CGPointMake(0., defaultHeight - kActionBarHeight - 1);
  [self.viewController.view addSubview:self.extendedPlacePageView];
  [self.viewController.view addSubview:self.actionBar];
  [view addSubview:self.navigationController.view];
}

- (void)show { }

- (void)dismiss
{
  [self.navigationController.view removeFromSuperview];
  [self.navigationController removeFromParentViewController];
  [super dismiss];
}

- (void)addBookmark
{
  [super addBookmark];
  self.navigationController.view.height += kBookmarkCellHeight;
  self.viewController.view.height += kBookmarkCellHeight;
  self.extendedPlacePageView.height += kBookmarkCellHeight;
  self.actionBar.minY += kBookmarkCellHeight;
}

- (void)removeBookmark
{
  [super removeBookmark];
  self.navigationController.view.height += kBookmarkCellHeight;
  self.viewController.view.height -= kBookmarkCellHeight;
  self.extendedPlacePageView.height -= kBookmarkCellHeight;
  self.actionBar.minY -= kBookmarkCellHeight;
}

- (void)changeBookmarkColor
{
  MWMBookmarkColorViewController * controller = [[MWMBookmarkColorViewController alloc] initWithNibName:[MWMBookmarkColorViewController className] bundle:nil];
  controller.ownerNavigationController = self.navigationController;
  controller.placePageManager = self.manager;
  controller.view.frame = self.viewController.view.frame;
  [self.viewController.navigationController pushViewController:controller animated:YES];
}

- (void)changeBookmarkCategory
{
  SelectSetVC * controller = [[SelectSetVC alloc] initWithPlacePageManager:self.manager];
  controller.ownerNavigationController = self.navigationController;
  controller.view.frame = self.viewController.view.frame;
  [self.viewController.navigationController pushViewController:controller animated:YES];
}

- (void)changeBookmarkDescription
{
  MWMBookmarkDescriptionViewController * controller = [[MWMBookmarkDescriptionViewController alloc] initWithPlacePageManager:self.manager];
  controller.ownerNavigationController = self.navigationController;
  [self.viewController.navigationController pushViewController:controller animated:YES];
}

@end
